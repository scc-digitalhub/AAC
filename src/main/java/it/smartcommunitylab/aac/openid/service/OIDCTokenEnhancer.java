
package it.smartcommunitylab.aac.openid.service;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.util.Assert;

import com.google.common.base.Strings;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.claims.ClaimsService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.core.service.ClientDetailsService;
import it.smartcommunitylab.aac.jwt.JWTService;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;

/**
 * Build an ID token via profile
 */

public class OIDCTokenEnhancer implements TokenEnhancer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String MAX_AGE = "max_age";
    public static final String NONCE = "nonce";
    public static final String AUTH_TIMESTAMP = "AUTH_TIMESTAMP";

    private final String issuer;

    // TODO evaluate drop claimService, idToken could be standard only
    // at minimum we should split claim mapping for access and id tokens
    private final ClaimsService claimsService;

    private final JWTService jwtService;

    private final ClientDetailsService clientDetailsService;
    private final OAuth2ClientDetailsService oauth2ClientDetailsService;

    public OIDCTokenEnhancer(String issuer, JWTService jwtService,
            ClientDetailsService clientDetailsService, OAuth2ClientDetailsService oauth2ClientDetailsService,
            ClaimsService claimsService) {
        Assert.hasText(issuer, "a valid issuer is required");
        Assert.notNull(jwtService, "jwt service is mandatory to sign tokens");
        Assert.notNull(clientDetailsService, "client details service is mandatory");
        Assert.notNull(oauth2ClientDetailsService, "oauth2 client details service is mandatory");
        Assert.notNull(claimsService, "claims service is mandatory");

        this.issuer = issuer;
        this.jwtService = jwtService;
        this.clientDetailsService = clientDetailsService;
        this.oauth2ClientDetailsService = oauth2ClientDetailsService;
        this.claimsService = claimsService;
    }

    @Override
    public AACOAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        logger.debug("enhance access token " + accessToken.getTokenType() + " for " + authentication.getName()
                + " value " + accessToken.toString());

        OAuth2Request request = authentication.getOAuth2Request();
        String clientId = request.getClientId();
        Set<String> scopes = request.getScope();

        AACOAuth2AccessToken token = new AACOAuth2AccessToken(accessToken);

        // evaluate if request is for an idToken
        if (!scopes.contains(Config.SCOPE_OPENID) || !request.getResponseTypes().contains("id_token")) {
            // nothing to do
            return token;
        }

        try {

            ClientDetails clientDetails = clientDetailsService.loadClient(clientId);
            OAuth2ClientDetails oauth2ClientDetails = oauth2ClientDetailsService.loadClientByClientId(clientId);

            logger.debug("fetch user via authentication");

            Authentication userAuth = authentication.getUserAuthentication();
            if (userAuth == null || !(userAuth instanceof UserAuthenticationToken)) {
                throw new InvalidRequestException("id_token requires a valid user authentication");
            }

            UserDetails userDetails = ((UserAuthenticationToken) userAuth).getUser();

            JWT idToken = createIdToken(request, accessToken, userDetails, clientDetails, oauth2ClientDetails);

            token.setIdToken(idToken);

            return token;

        } catch (NoSuchClientException | ClientRegistrationException e) {
            logger.error("non existing client: " + e.getMessage());
            throw new InvalidClientException("invalid client");
        } catch (SystemException | NoSuchResourceException | InvalidDefinitionException e) {
            logger.error("claims service error: " + e.getMessage());
            throw new OAuth2Exception(e.getMessage());
        }
    }

    private JWT createIdToken(OAuth2Request request, OAuth2AccessToken accessToken,
            UserDetails userDetails,
            ClientDetails clientDetails, OAuth2ClientDetails oauth2ClientDetails)
            throws NoSuchResourceException, InvalidDefinitionException, SystemException {

        logger.trace("access token used for oidc is " + accessToken);

        String clientId = clientDetails.getClientId();
        String subjectId = userDetails.getSubjectId();
        Set<String> scopes = request.getScope();
        Set<String> resourceIds = request.getResourceIds();

        // build claims set according to OIDC 1.0
        JWTClaimsSet.Builder idClaims = new JWTClaimsSet.Builder();

        // ask claim Manager for user claims
        // TODO evaluate splitting claims from accessToken or dropping extended claims
        // on idToken. Besides we already have claims here, why do again if we get the
        // same result
        Map<String, Serializable> userClaims = claimsService.getUserClaims(userDetails, clientDetails, scopes,
                resourceIds);

        // set via builder
        userClaims.entrySet().forEach(e -> idClaims.claim(e.getKey(), e.getValue()));

        // if the auth time claim was explicitly requested OR if the client always wants
        // the auth time, put it in
        // TODO check "idtoken" vs "id_token"
        if (request.getExtensions().containsKey(MAX_AGE) || (request.getExtensions().containsKey("idtoken"))) {

            if (request.getExtensions().get(AUTH_TIMESTAMP) != null) {

                Long authTimestamp = Long.parseLong((String) request.getExtensions().get(AUTH_TIMESTAMP));
                if (authTimestamp != null) {
                    idClaims.claim("auth_time", authTimestamp / 1000L);
                }
            } else {
                // we couldn't find the timestamp!
                logger.warn(
                        "Unable to find authentication timestamp! There is likely something wrong with the configuration.");
            }
        }

        idClaims.issueTime(new Date());

        if (accessToken.getExpiration() != null) {
            Date expiration = accessToken.getExpiration();
            idClaims.expirationTime(expiration);
        }

        // DISABLED multiple audiences same as accessToken
//        List<String> audiences = new LinkedList<>();
//        audiences.add(clientId);
//        audiences.addAll(getServiceIds(request.getScope()));
//      idClaims.audience(audiences);

        // set single audience as string - correct for OIDC
        idClaims.audience(clientId);

        idClaims.issuer(issuer);
        idClaims.subject(subjectId);
        idClaims.jwtID(UUID.randomUUID().toString());
        idClaims.claim("azp", clientId);

        String nonce = (String) request.getExtensions().get(NONCE);
        if (!Strings.isNullOrEmpty(nonce)) {
            idClaims.claim("nonce", nonce);
        }

        // add additional claims for scopes
        // DISABLED, not in the spec
//		idClaims.claim("scope", String.join(" ", request.getScope()));

        Set<String> responseTypes = request.getResponseTypes();

        // at_hash is used for both implicit and auth_code flows when paired with
        // accessToken
        if (responseTypes.contains("token") || responseTypes.contains("code")) {

            // calculate the token hash
            Base64URL at_hash = jwtService.hashAccessToken(oauth2ClientDetails, accessToken.getValue());
            idClaims.claim("at_hash", at_hash);
        }

        JWTClaimsSet claims = idClaims.build();
        logger.trace("idToken claims " + claims.toString());

        JWT idToken = jwtService.buildAndSignJWT(oauth2ClientDetails, claims);

        logger.trace("idToken result " + idToken.serialize());

        return idToken;

    }

}