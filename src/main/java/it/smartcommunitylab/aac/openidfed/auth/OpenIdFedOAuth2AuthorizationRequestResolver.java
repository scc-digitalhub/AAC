/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.openidfed.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.oidc.apple.provider.AppleIdentityProviderConfig;
import it.smartcommunitylab.aac.oidc.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfigMap;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

/*
 * Extends spring DefaultOAuth2AuthorizationRequestResolver for OpenId Fed
 *
 * we could also leverage request customizer to accomplish the same
 *
 */

public class OpenIdFedOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final int DEFAULT_DURATION = 300;

    private final OAuth2AuthorizationRequestResolver defaultResolver;
    private final ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository;
    private final RequestMatcher requestMatcher;

    public OpenIdFedOAuth2AuthorizationRequestResolver(
        ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository,
        ClientRegistrationRepository clientRegistrationRepository,
        String authorizationRequestBaseUri
    ) {
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
        Assert.hasText(authorizationRequestBaseUri, "authorizationRequestBaseUri cannot be empty");

        this.registrationRepository = registrationRepository;
        this.requestMatcher = new AntPathRequestMatcher(authorizationRequestBaseUri + "/{registrationId}");

        defaultResolver =
            new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, authorizationRequestBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest servletRequest) {
        // let default resolver work
        OAuth2AuthorizationRequest request = defaultResolver.resolve(servletRequest);

        // resolver providerId and load config
        String providerId = resolveRegistrationId(servletRequest);
        if (providerId != null) {
            OpenIdFedIdentityProviderConfig config = registrationRepository.findByProviderId(providerId);
            // add parameters
            return extendAuthorizationRequest(request, config);
        }

        return null;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest servletRequest, String clientRegistrationId) {
        // let default resolver work
        OAuth2AuthorizationRequest request = defaultResolver.resolve(servletRequest, clientRegistrationId);

        // resolver providerId and load config
        String providerId = resolveRegistrationId(servletRequest);
        if (providerId != null) {
            OpenIdFedIdentityProviderConfig config = registrationRepository.findByProviderId(providerId);
            // add parameters
            return extendAuthorizationRequest(request, config);
        }

        return null;
    }

    private String resolveRegistrationId(HttpServletRequest request) {
        if (this.requestMatcher.matches(request)) {
            return this.requestMatcher.matcher(request).getVariables().get("registrationId");
        }
        return null;
    }

    private OAuth2AuthorizationRequest extendAuthorizationRequest(
        OAuth2AuthorizationRequest authRequest,
        OpenIdFedIdentityProviderConfig config
    ) {
        if (authRequest == null || config == null) {
            return null;
        }

        // we support only authcode
        if (!AuthorizationGrantType.AUTHORIZATION_CODE.equals(authRequest.getGrantType())) {
            return null;
        }

        // fetch paramers from resolved request
        Map<String, Object> attributes = new HashMap<>(authRequest.getAttributes());
        Map<String, Object> additionalParameters = new HashMap<>(authRequest.getAdditionalParameters());

        //add pkce
        addPkceParameters(attributes, additionalParameters);

        //build request object
        addRequestObject(config, attributes, additionalParameters);

        // get a builder and reset paramers
        return OAuth2AuthorizationRequest
            .from(authRequest)
            .attributes(attributes)
            .additionalParameters(additionalParameters)
            .build();
    }

    /*
     * add pkce parameters, copy from DefaultOAuth2AuthorizationRequestResolver
     */
    private final StringKeyGenerator secureKeyGenerator = new Base64StringKeyGenerator(
        Base64.getUrlEncoder().withoutPadding(),
        96
    );

    private void addPkceParameters(Map<String, Object> attributes, Map<String, Object> additionalParameters) {
        String codeVerifier = this.secureKeyGenerator.generateKey();
        attributes.put(PkceParameterNames.CODE_VERIFIER, codeVerifier);
        try {
            String codeChallenge = createS256Hash(codeVerifier);
            additionalParameters.put(PkceParameterNames.CODE_CHALLENGE, codeChallenge);
            additionalParameters.put(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256");
        } catch (NoSuchAlgorithmException e) {
            additionalParameters.put(PkceParameterNames.CODE_CHALLENGE, codeVerifier);
        }
    }

    private static String createS256Hash(String value) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(value.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    /*
     * Request object
     */
    private void addRequestObject(
        OpenIdFedIdentityProviderConfig config,
        Map<String, Object> attributes,
        Map<String, Object> additionalParameters
    ) {
        OpenIdFedIdentityProviderConfigMap configMap = config.getConfigMap();
        JWK jwk = config.getClientJWK();

        if (jwk == null || !jwk.isPrivate()) {
            return;
        }

        // we support only automatic registration
        // build jwt with assertions as per
        // https://openid.net/specs/openid-federation-1_0.html#name-authentication-request
        try {
            JWSAlgorithm jwsAlgorithm = resolveAlgorithm(jwk);
            if (jwsAlgorithm == null) {
                return;
            }

            JWSSigner signer = buildJwsSigner(jwk, jwsAlgorithm);
            if (signer == null) {
                return;
            }

            JWSHeader header = new JWSHeader.Builder(jwsAlgorithm).keyID(jwk.getKeyID()).build();

            Instant issuedAt = Instant.now();
            Instant expiresAt = issuedAt.plus(Duration.ofSeconds(DEFAULT_DURATION));

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(configMap.getClientId())
                .audience(Collections.singletonList(configMap.getIssuerUri()))
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                //TODO add trust chain which should be stored in providerConfig
                .build();

            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);

            // add to parameters
            attributes.put("request", jwt.serialize());
            // set as request object
            additionalParameters.put("request", jwt.serialize());
        } catch (JOSEException e) {
            throw new JwtEncodingException("error encoding the jwt with the provided key");
        }
    }

    private JWSSigner buildJwsSigner(JWK jwk, JWSAlgorithm jwsAlgorithm) throws JOSEException {
        if (KeyType.RSA.equals(jwk.getKeyType())) {
            return new RSASSASigner(jwk.toRSAKey());
        } else if (KeyType.EC.equals(jwk.getKeyType())) {
            return new ECDSASigner(jwk.toECKey());
        } else if (KeyType.OCT.equals(jwk.getKeyType())) {
            return new MACSigner(jwk.toOctetSequenceKey());
        }

        return null;
    }

    private JWSAlgorithm resolveAlgorithm(JWK jwk) {
        String jwsAlgorithm = null;
        if (jwk.getAlgorithm() != null) {
            jwsAlgorithm = jwk.getAlgorithm().getName();
        }

        if (jwsAlgorithm == null) {
            if (KeyType.RSA.equals(jwk.getKeyType())) {
                jwsAlgorithm = SignatureAlgorithm.RS256.getName();
            } else if (KeyType.EC.equals(jwk.getKeyType())) {
                jwsAlgorithm = SignatureAlgorithm.ES256.getName();
            } else if (KeyType.OCT.equals(jwk.getKeyType())) {
                jwsAlgorithm = MacAlgorithm.HS256.getName();
            }
        }

        return jwsAlgorithm == null ? null : JWSAlgorithm.parse(jwsAlgorithm);
    }
}
