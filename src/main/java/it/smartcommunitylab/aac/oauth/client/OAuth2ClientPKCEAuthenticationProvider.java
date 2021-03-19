package it.smartcommunitylab.aac.oauth.client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.oauth.ClientPKCEAuthenticationToken;
import it.smartcommunitylab.aac.oauth.PeekableAuthorizationCodeServices;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientUserDetailsService;

public class OAuth2ClientPKCEAuthenticationProvider implements AuthenticationProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OAuth2ClientDetailsService clientDetailsService;

    // we need to peek at authCodes to load request and verify auth
    private final PeekableAuthorizationCodeServices authCodeServices;

    public OAuth2ClientPKCEAuthenticationProvider(
            OAuth2ClientDetailsService clientDetailsService,
            PeekableAuthorizationCodeServices authCodeServices) {
        Assert.notNull(authCodeServices, "authCode services is required");
        Assert.notNull(clientDetailsService, "client details service is required");
        this.clientDetailsService = clientDetailsService;
        this.authCodeServices = authCodeServices;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(ClientPKCEAuthenticationToken.class, authentication,
                "Only ClientPKCEAuthenticationToken is supported");

        ClientPKCEAuthenticationToken authRequest = (ClientPKCEAuthenticationToken) authentication;
        String clientId = authRequest.getPrincipal();
        String code = authRequest.getCode();
        String codeVerifier = authRequest.getCodeVerifier();

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(code) || !StringUtils.hasText(codeVerifier)) {
            throw new BadCredentialsException("missing required parameters in request");
        }

        /*
         * We authenticate clients by checking if code exists, verifier matches code and
         * if the code is assigned to the same client
         */

        OAuth2Authentication oauth = authCodeServices.peekAuthorizationCode(code);
        if (oauth == null) {
            // don't leak
            throw new BadCredentialsException("invalid request");
        }

        OAuth2Request pendingOAuth2Request = oauth.getOAuth2Request();

        if (!pendingOAuth2Request.getClientId().equals(clientId)) {
            // client id does not match
            throw new BadCredentialsException("invalid request");
        }

        // check challenge
        String codeChallenge = pendingOAuth2Request.getRequestParameters().get(PkceParameterNames.CODE_CHALLENGE);
        String codeChallengeMethod = pendingOAuth2Request.getRequestParameters()
                .get(PkceParameterNames.CODE_CHALLENGE_METHOD);

        // we need to be sure this is a PKCE request
        if (!StringUtils.hasText(codeChallenge) || !StringUtils.hasText(codeChallengeMethod)) {
            // this is NOT a PKCE authcode
            throw new BadCredentialsException("invalid request");
        }

        // validate challenge+verifier
        if (!getCodeChallenge(codeVerifier, codeChallengeMethod).equals(codeChallenge)) {
            throw new BadCredentialsException("invalid request");
        }

        // client auth is valid for this request, load details
        OAuth2ClientDetails client = clientDetailsService.loadClientByClientId(clientId);

        // result contains credentials, someone later on will need to call
        // eraseCredentials
        ClientPKCEAuthenticationToken result = new ClientPKCEAuthenticationToken(clientId, code, codeVerifier,
                client.getAuthorities());
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (ClientPKCEAuthenticationToken.class.isAssignableFrom(authentication));
    }

    /**
     * Generates the code challenge from a given code verifier and code challenge
     * method.
     * 
     * @param codeVerifier
     * @param codeChallengeMethod allowed values are only <code>plain</code> and
     *                            <code>S256</code>
     * @return
     */
    private static String getCodeChallenge(String codeVerifier, String codeChallengeMethod) {
        try {
            if (codeChallengeMethod.equals("plain")) {
                return codeVerifier;
            } else if (codeChallengeMethod.equalsIgnoreCase("S256")) {
                return createS256Hash(codeVerifier);
            } else {
                throw new IllegalArgumentException(codeChallengeMethod + " is not a supported code challenge method.");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("No such algorithm [SHA-256]");
        }
    }

    private static String createS256Hash(String value) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(value.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}
