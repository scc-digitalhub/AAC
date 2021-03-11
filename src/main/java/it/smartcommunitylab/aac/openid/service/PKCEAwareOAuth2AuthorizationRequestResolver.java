package it.smartcommunitylab.aac.openid.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.util.Assert;

/*
 * Extends spring DefaultOAuth2AuthorizationRequestResolver to always include PKCE exchange
 * TODO remove class when lands https://github.com/spring-projects/spring-security/pull/7804
 * 
 * we could also leverage request customizer to accomplish the same
 * 
 */

public class PKCEAwareOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public PKCEAwareOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository,
            String authorizationRequestBaseUri) {
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
        Assert.hasText(authorizationRequestBaseUri, "authorizationRequestBaseUri cannot be empty");

        defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository,
                authorizationRequestBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest servletRequest) {
        // let default resolver work
        OAuth2AuthorizationRequest request = defaultResolver.resolve(servletRequest);

        // add pkce
        return extendAuthorizationRequest(request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest servletRequest, String clientRegistrationId) {
        // let default resolver work
        OAuth2AuthorizationRequest request = defaultResolver.resolve(servletRequest, clientRegistrationId);

        // add pkce
        return extendAuthorizationRequest(request);
    }

    private OAuth2AuthorizationRequest extendAuthorizationRequest(OAuth2AuthorizationRequest authRequest) {
        if (authRequest == null) {
            return null;
        }

        // we support only authcode
        if (!AuthorizationGrantType.AUTHORIZATION_CODE.equals(authRequest.getGrantType())) {
            return null;
        }

        // fetch paramers from resolved request
        Map<String, Object> attributes = new HashMap<>(authRequest.getAttributes());
        Map<String, Object> additionalParameters = new HashMap<>(authRequest.getAdditionalParameters());
        addPkceParameters(attributes, additionalParameters);

        // get a builder and reset paramers
        return OAuth2AuthorizationRequest.from(authRequest)
                .attributes(attributes)
                .additionalParameters(additionalParameters)
                .build();
    }

    /*
     * add pkce parameters, copy from DefaultOAuth2AuthorizationRequestResolver
     */
    private final StringKeyGenerator secureKeyGenerator = new Base64StringKeyGenerator(
            Base64.getUrlEncoder().withoutPadding(), 96);

    private void addPkceParameters(Map<String, Object> attributes, Map<String, Object> additionalParameters) {
        String codeVerifier = this.secureKeyGenerator.generateKey();
        attributes.put(PkceParameterNames.CODE_VERIFIER, codeVerifier);
        try {
            String codeChallenge = createHash(codeVerifier);
            additionalParameters.put(PkceParameterNames.CODE_CHALLENGE, codeChallenge);
            additionalParameters.put(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256");
        } catch (NoSuchAlgorithmException e) {
            additionalParameters.put(PkceParameterNames.CODE_CHALLENGE, codeVerifier);
        }
    }

    private static String createHash(String value) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(value.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}