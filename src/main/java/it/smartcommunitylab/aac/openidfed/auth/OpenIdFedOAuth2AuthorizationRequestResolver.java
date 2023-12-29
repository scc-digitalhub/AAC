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
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import it.smartcommunitylab.aac.openidfed.service.DefaultOpenIdRpMetadataResolver;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

// TODO leverage request customizer in place of rebuilding the request
public class OpenIdFedOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final int DEFAULT_DURATION = 300;

    private final ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository;
    private final RequestMatcher requestMatcher;
    private final String authorizationRequestBaseUri;

    public OpenIdFedOAuth2AuthorizationRequestResolver(
        ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository,
        String authorizationRequestBaseUri
    ) {
        Assert.notNull(registrationRepository, "registrationRepository cannot be null");
        Assert.hasText(authorizationRequestBaseUri, "authorizationRequestBaseUri cannot be empty");

        this.registrationRepository = registrationRepository;
        this.authorizationRequestBaseUri = authorizationRequestBaseUri;

        this.requestMatcher = new AntPathRequestMatcher(authorizationRequestBaseUri + "/{providerId}/{registrationId}");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest servletRequest) {
        //resolve from request
        String providerId = resolveProviderId(servletRequest);
        String registrationId = resolveRegistrationId(servletRequest);

        return resolve(servletRequest, providerId, registrationId);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest servletRequest, String clientRegistrationId) {
        //resolve from request
        String providerId = resolveProviderId(servletRequest);

        return resolve(servletRequest, providerId, clientRegistrationId);
    }

    private OAuth2AuthorizationRequest resolve(
        HttpServletRequest servletRequest,
        String providerId,
        String registrationId
    ) {
        if (providerId == null || registrationId == null) {
            return null;
        }

        //load config
        OpenIdFedIdentityProviderConfig config = registrationRepository.findByProviderId(providerId);
        if (config == null) {
            return null;
        }

        //build a scoped default resolver
        DefaultOAuth2AuthorizationRequestResolver defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
            config.getClientRegistrationRepository(),
            authorizationRequestBaseUri + "/" + providerId
        );

        //add PKCE
        defaultResolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

        // let default resolver work
        OAuth2AuthorizationRequest request = defaultResolver.resolve(servletRequest);

        // add parameters
        return extendAuthorizationRequest(servletRequest, request, config, registrationId);
    }

    private OAuth2AuthorizationRequest extendAuthorizationRequest(
        HttpServletRequest servletRequest,
        OAuth2AuthorizationRequest authRequest,
        OpenIdFedIdentityProviderConfig config,
        String registrationId
    ) {
        if (authRequest == null || config == null || registrationId == null) {
            return null;
        }

        ClientRegistration registration = config.getClientRegistrationRepository().findByRegistrationId(registrationId);
        if (registration == null) {
            return null;
        }

        // we support only authcode
        if (!AuthorizationGrantType.AUTHORIZATION_CODE.equals(authRequest.getGrantType())) {
            return null;
        }

        //resolve and overwrite clientId
        String baseUrl = DefaultOpenIdRpMetadataResolver.extractBaseUrl(servletRequest);
        String clientId = DefaultOpenIdRpMetadataResolver
            .expandRedirectUri(baseUrl, config.getClientId(), "id")
            .toString();

        // fetch paramers from resolved request
        Map<String, Object> attributes = new HashMap<>(authRequest.getAttributes());
        Map<String, Object> additionalParameters = new HashMap<>(authRequest.getAdditionalParameters());

        //add individual claims request
        addClaimsRequest(authRequest, config, clientId, registration, attributes, additionalParameters);

        //add request object
        addRequestObject(authRequest, config, clientId, registration, attributes, additionalParameters);

        // get a builder and reset paramers
        return OAuth2AuthorizationRequest
            .from(authRequest)
            .clientId(clientId)
            .attributes(attributes)
            .additionalParameters(additionalParameters)
            .build();
    }

    /*
     * Indivual claims
     */
    private void addClaimsRequest(
        OAuth2AuthorizationRequest authRequest,
        OpenIdFedIdentityProviderConfig config,
        String clientId,
        ClientRegistration registration,
        Map<String, Object> attributes,
        Map<String, Object> additionalParameters
    ) {
        Set<String> claims = config.getConfigMap().getClaims();
        if (claims != null && !claims.isEmpty()) {
            //build request, we treat every requested claim as `essential`
            //build for userinfo only, we always read from there
            Map<String, Object> map = new HashMap<>();

            Map<Object, Object> req = claims
                .stream()
                .map(c -> {
                    Map<String, Serializable> r = Map.of("essential", true);
                    return Map.entry(c, r);
                })
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

            map.put("userinfo", req);

            additionalParameters.put("claims", map);
        }
    }

    /*
     * Request object
     */
    private void addRequestObject(
        OAuth2AuthorizationRequest authRequest,
        OpenIdFedIdentityProviderConfig config,
        String clientId,
        ClientRegistration registration,
        Map<String, Object> attributes,
        Map<String, Object> additionalParameters
    ) {
        JWK jwk = config.getClientSignatureJWK();

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

            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(clientId)
                .audience(Collections.singletonList(registration.getProviderDetails().getIssuerUri()))
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt));
            //TODO add trust chain which should be stored in providerConfig
            //TODO add acr_values

            //add all request parameters into claims OAuth2ParameterNames
            claims
                //oauth2
                .claim(OAuth2ParameterNames.CLIENT_ID, clientId)
                .claim(OAuth2ParameterNames.REDIRECT_URI, authRequest.getRedirectUri())
                .claim(OAuth2ParameterNames.RESPONSE_TYPE, authRequest.getResponseType().getValue())
                .claim(OAuth2ParameterNames.SCOPE, String.join(" ", authRequest.getScopes()))
                .claim(OAuth2ParameterNames.STATE, authRequest.getState())
                //oidc
                .claim(OidcParameterNames.NONCE, additionalParameters.get(OidcParameterNames.NONCE))
                //pkce
                .claim(PkceParameterNames.CODE_CHALLENGE, additionalParameters.get(PkceParameterNames.CODE_CHALLENGE))
                .claim(
                    PkceParameterNames.CODE_CHALLENGE_METHOD,
                    additionalParameters.get(PkceParameterNames.CODE_CHALLENGE_METHOD)
                );

            if (config.getConfigMap().getAcrValues() != null && !config.getConfigMap().getAcrValues().isEmpty()) {
                claims.claim("acr_values", config.getConfigMap().getAcrValues());
            }

            if (additionalParameters.containsKey("claims")) {
                //add individual claims also to request obj
                claims.claim("claims", additionalParameters.get("claims"));
            }

            SignedJWT jwt = new SignedJWT(header, claims.build());
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

    private String resolveRegistrationId(HttpServletRequest request) {
        if (this.requestMatcher.matches(request)) {
            return this.requestMatcher.matcher(request).getVariables().get("registrationId");
        }
        return null;
    }

    private String resolveProviderId(HttpServletRequest request) {
        if (this.requestMatcher.matches(request)) {
            return this.requestMatcher.matcher(request).getVariables().get("providerId");
        }
        return null;
    }
}
