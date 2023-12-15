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
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class OpenIdFedClientAuthenticationParametersConverter
    extends OAuth2AuthorizationCodeGrantRequestEntityConverter {

    private static final int DEFAULT_DURATION = 180;

    private final String clientId;
    private final JWSSigner signer;

    public OpenIdFedClientAuthenticationParametersConverter(String clientId, JWK jwk) {
        Assert.hasText(clientId, "clientId is required");
        Assert.notNull(jwk, "jwk must be set");

        this.clientId = clientId;

        try {
            JWSAlgorithm jwsAlgorithm = resolveAlgorithm(jwk);
            if (jwsAlgorithm == null) {
                throw new IllegalArgumentException();
            }

            this.signer = buildJwsSigner(jwk, jwsAlgorithm);
            if (this.signer == null) {
                throw new IllegalArgumentException();
            }
        } catch (JOSEException e) {
            throw new IllegalArgumentException("invalid private key");
        }
    }

    @Override
    protected MultiValueMap<String, String> createParameters(
        OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest
    ) {
        Assert.notNull(authorizationCodeGrantRequest, "authorizationCodeGrantRequest cannot be null");

        ClientRegistration clientRegistration = authorizationCodeGrantRequest.getClientRegistration();
        OAuth2AuthorizationExchange authorizationExchange = authorizationCodeGrantRequest.getAuthorizationExchange();

        //build base params
        MultiValueMap<String, String> parameters = super.createParameters(authorizationCodeGrantRequest);

        // build client assertion
        // try {
            // JWSHeader header = new JWSHeader.Builder(jwsAlgorithm).keyID(jwk.getKeyID()).build();

            // Instant issuedAt = Instant.now();
            // Instant expiresAt = issuedAt.plus(Duration.ofSeconds(DEFAULT_DURATION));

            // JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
            //     .issuer(clientId)
            //     .audience(Collections.singletonList(registration.getProviderDetails().getIssuerUri()))
            //     .jwtID(UUID.randomUUID().toString())
            //     .issueTime(Date.from(issuedAt))
            //     .expirationTime(Date.from(expiresAt));

            // JWTClaimsSet claims = new JWTClaimsSet.Builder()
            //     .issuer(configMap.getTeamId())
            //     .subject(clientRegistration.getClientId())
            //     .audience(Collections.singletonList(AppleIdentityProviderConfig.ISSUER_URI))
            //     .jwtID(UUID.randomUUID().toString())
            //     .issueTime(Date.from(issuedAt))
            //     .expirationTime(Date.from(expiresAt))
            //     .build();

            // SignedJWT jwt = new SignedJWT(header, claims);
            // jwt.sign(signer);

            // // set as secret
            // parameters.set(OAuth2ParameterNames.CLIENT_ASSERTION, jwt.serialize());
            // parameters.set(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE, AuthorizationGrantType.B)
        // } catch (JOSEException e) {
        //     throw new JwtEncodingException("error encoding the jwt with the provided key");
        // }

        return parameters;
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
