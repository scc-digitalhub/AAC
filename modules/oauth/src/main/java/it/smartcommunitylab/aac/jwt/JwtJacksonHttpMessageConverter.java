/**
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

package it.smartcommunitylab.aac.jwt;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/*
 * Jwt http message converter using JwtDecoder
 */

public class JwtJacksonHttpMessageConverter extends TypeConstrainedMappingJackson2HttpMessageConverter {

    private static MediaType[] SUPPORTED_MEDIA_TYPES = {
        new MediaType("application", "jwt"),
        new MediaType("application", "jose"),
    };

    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final NimbusJwtDecoder decoder;

    public JwtJacksonHttpMessageConverter(String jwksUri) {
        super(Map.class, Arrays.asList(SUPPORTED_MEDIA_TYPES), objectMapper);
        Assert.hasText(jwksUri, "jwks set uri is required");
        decoder = new JwtDecoderBuilder().jwksUri(jwksUri).build();
    }

    public JwtJacksonHttpMessageConverter(JWKSet jwks) {
        super(Map.class, Arrays.asList(SUPPORTED_MEDIA_TYPES), objectMapper);
        Assert.notNull(jwks, "jwks is required");
        decoder = new JwtDecoderBuilder().jwks(jwks).build();
    }

    public JwtJacksonHttpMessageConverter(String jwksUri, JWK jweKey, String jweAlgorithm, String jweMethod) {
        super(Map.class, Arrays.asList(SUPPORTED_MEDIA_TYPES), objectMapper);
        Assert.notNull(jweKey, "jwe key is required");
        Assert.hasText(jwksUri, "jwks set uri is required");
        Assert.hasText(jweAlgorithm, "jweAlgorithm is required");
        Assert.hasText(jweMethod, "jweMethod is required");

        decoder = new JwtDecoderBuilder()
            .jwksUri(jwksUri)
            .jweKey(jweKey)
            .jweAlgorithm(JWEAlgorithm.parse(jweAlgorithm))
            .jweMethod(EncryptionMethod.parse(jweMethod))
            .build();
    }

    public JwtJacksonHttpMessageConverter(JWKSet jwks, JWK jweKey, String jweAlgorithm, String jweMethod) {
        super(Map.class, Arrays.asList(SUPPORTED_MEDIA_TYPES), objectMapper);
        Assert.notNull(jweKey, "jwe key is required");
        Assert.notNull(jwks, "jwks uri is required");
        Assert.hasText(jweAlgorithm, "jweAlgorithm is required");
        Assert.hasText(jweMethod, "jweMethod is required");

        decoder = new JwtDecoderBuilder()
            .jwks(jwks)
            .jweKey(jweKey)
            .jweAlgorithm(JWEAlgorithm.parse(jweAlgorithm))
            .jweMethod(EncryptionMethod.parse(jweMethod))
            .build();
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Map.class == clazz;
    }

    @Override
    public Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException {
        JavaType javaType = getJavaType(type, contextClass);
        return doRead(inputMessage);
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException {
        JavaType javaType = getJavaType(clazz, null);
        return doRead(inputMessage);
    }

    protected Map<String, Object> doRead(HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException {
        try {
            String body = StreamUtils.copyToString(inputMessage.getBody(), StandardCharsets.UTF_8);
            Jwt jwt = decoder.decode(body);
            return jwt.getClaims();
        } catch (JwtException e) {
            throw new HttpMessageNotReadableException("not a valid jwt", inputMessage);
        }
    }

    @Override
    protected void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
        throws IOException, HttpMessageNotWritableException {
        // TODO
        throw new UnsupportedOperationException();
    }
}
