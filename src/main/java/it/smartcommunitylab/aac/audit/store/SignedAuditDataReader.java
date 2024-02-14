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

import com.nimbusds.jose.jwk.JWKSet;
import it.smartcommunitylab.aac.jwt.JwtDecoderBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;

public class SignedAuditDataReader implements Converter<byte[], Map<String, Object>> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String issuer;
    private final JWKSet jwks;

    private NimbusJwtDecoder decoder;

    public SignedAuditDataReader(String issuer, JWKSet jwks) {
        Assert.hasText(issuer, "issuer is required");
        Assert.notNull(jwks, "jwks can not be null");

        this.issuer = issuer;
        this.jwks = jwks;

        this.decoder = new JwtDecoderBuilder().jwks(jwks).build();
        this.decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
    }

    public void setSignatureAlgorithm(SignatureAlgorithm signatureAlgorithm) {
        //rebuild decoder
        this.decoder = new JwtDecoderBuilder().jwks(jwks).jwsAlgorithm(signatureAlgorithm).build();
        this.decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
    }

    @Override
    public Map<String, Object> convert(byte[] source) {
        if (this.decoder == null) {
            throw new RuntimeException("invalid or missing configuration");
        }

        try {
            String token = new String(source, StandardCharsets.UTF_8);
            Jwt jwt = decoder.decode(token);
            return jwt.getClaims();
        } catch (JwtException e) {
            logger.error("error converting the map with the provided configuration: {}", e.getMessage());
            throw new IllegalArgumentException("error converting the map with the provided configuration", e);
        }
    }
}
