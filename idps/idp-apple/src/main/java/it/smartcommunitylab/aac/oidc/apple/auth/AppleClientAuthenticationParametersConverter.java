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

package it.smartcommunitylab.aac.oidc.apple.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import it.smartcommunitylab.aac.oidc.apple.provider.AppleIdentityProviderConfig;
import it.smartcommunitylab.aac.oidc.apple.provider.AppleIdentityProviderConfigMap;
import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class AppleClientAuthenticationParametersConverter
    implements Converter<OAuth2AuthorizationCodeGrantRequest, MultiValueMap<String, String>> {

    private static final int DEFAULT_DURATION = 180;

    private final AppleIdentityProviderConfigMap configMap;
    private final JWSSigner jwsSigner;

    public AppleClientAuthenticationParametersConverter(AppleIdentityProviderConfig config) {
        Assert.notNull(config, "config must be set");
        this.configMap = config.getConfigMap();
        ECPrivateKey privateKey = config.getPrivateKey();
        Assert.notNull(privateKey, "private key must be set");

        try {
            // build signer with this key
            this.jwsSigner = new ECDSASigner(privateKey);
        } catch (JOSEException e) {
            throw new IllegalArgumentException("invalid private key");
        }
    }

    @Override
    public MultiValueMap<String, String> convert(OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest) {
        Assert.notNull(authorizationCodeGrantRequest, "authorizationCodeGrantRequest cannot be null");

        ClientRegistration clientRegistration = authorizationCodeGrantRequest.getClientRegistration();
        OAuth2AuthorizationExchange authorizationExchange = authorizationCodeGrantRequest.getAuthorizationExchange();

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

        // add base parameters
        // https://developer.apple.com/documentation/sign_in_with_apple/generate_and_validate_tokens
        parameters.add(OAuth2ParameterNames.GRANT_TYPE, authorizationCodeGrantRequest.getGrantType().getValue());
        parameters.add(OAuth2ParameterNames.CODE, authorizationExchange.getAuthorizationResponse().getCode());
        parameters.add(
            OAuth2ParameterNames.REDIRECT_URI,
            authorizationExchange.getAuthorizationRequest().getRedirectUri()
        );
        parameters.add(OAuth2ParameterNames.CLIENT_ID, clientRegistration.getClientId());

        // build jwt with assertions as per
        // https://developer.apple.com/documentation/sign_in_with_apple/generate_and_validate_tokens
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(configMap.getKeyId()).build();

            Instant issuedAt = Instant.now();
            Instant expiresAt = issuedAt.plus(Duration.ofSeconds(DEFAULT_DURATION));

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(configMap.getTeamId())
                .subject(clientRegistration.getClientId())
                .audience(Collections.singletonList(AppleIdentityProviderConfig.ISSUER_URI))
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();

            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(jwsSigner);

            // set as secret
            parameters.set(OAuth2ParameterNames.CLIENT_SECRET, jwt.serialize());
        } catch (JOSEException e) {
            throw new JwtEncodingException("error encoding the jwt with the provided key");
        }

        return parameters;
    }
}
