/**
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.audit.store;

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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.util.Assert;

public class SignedAuditDataWriter implements Converter<Map<String, Object>, byte[]> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String issuer;
    private final JWK jwk;

    private JWSSigner signer;
    private JWSAlgorithm jwsAlgorithm;

    public SignedAuditDataWriter(String issuer, JWK jwk) {
        Assert.hasText(issuer, "issuer is required");
        Assert.notNull(jwk, "jwk can not be null");

        this.issuer = issuer;
        this.jwk = jwk;

        try {
            this.jwsAlgorithm = resolveAlgorithm(jwk);
            this.signer = buildJwsSigner(jwk);
        } catch (JOSEException e) {
            logger.error("error processing key {}", e.getMessage());
        }

        if (signer == null || jwsAlgorithm == null) {
            logger.error("error building signer from key");
            throw new IllegalArgumentException("error building signer from key");
        }
    }

    @Override
    public byte[] convert(Map<String, Object> source) {
        if (this.signer == null || jwsAlgorithm == null) {
            throw new RuntimeException("invalid or missing configuration");
        }

        try {
            JWSHeader header = new JWSHeader.Builder(jwsAlgorithm).keyID(jwk.getKeyID()).build();
            Instant issuedAt = Instant.now();

            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(issuedAt));

            if (source != null) {
                source.entrySet().forEach(e -> claims.claim(e.getKey(), e.getValue()));
            }

            SignedJWT jwt = new SignedJWT(header, claims.build());
            jwt.sign(signer);

            return jwt.serialize().getBytes(StandardCharsets.UTF_8);
        } catch (JOSEException e) {
            logger.error("error converting the map with the provided configuration: {}", e.getMessage());
            throw new IllegalArgumentException("error converting the map with the provided configuration", e);
        }
    }

    private static JWSSigner buildJwsSigner(JWK jwk) throws JOSEException {
        if (KeyType.RSA.equals(jwk.getKeyType())) {
            return new RSASSASigner(jwk.toRSAKey());
        } else if (KeyType.EC.equals(jwk.getKeyType())) {
            return new ECDSASigner(jwk.toECKey());
        } else if (KeyType.OCT.equals(jwk.getKeyType())) {
            return new MACSigner(jwk.toOctetSequenceKey());
        }

        return null;
    }

    private static JWSAlgorithm resolveAlgorithm(JWK jwk) {
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
