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

package it.smartcommunitylab.aac.openidfed.utils;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSetCache;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWEDecryptionKeySelector;
import com.nimbusds.jose.proc.JWEKeySelector;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.cache.Cache;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/*
 * Jwt http message converter using
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
        decoder = new JwkSetUriJwtDecoderBuilder(jwksUri).build();
    }

    public JwtJacksonHttpMessageConverter(String jwksUri, JWK jweKey, String jweAlgorithm, String jweMethod) {
        super(Map.class, Arrays.asList(SUPPORTED_MEDIA_TYPES), objectMapper);
        Assert.notNull(jweKey, "jwe key is required");
        Assert.hasText(jwksUri, "jwks set uri is required");
        Assert.hasText(jweAlgorithm, "jweAlgorithm is required");
        Assert.hasText(jweMethod, "jweMethod is required");

        decoder =
            new JwkSetUriJwtDecoderBuilder(jwksUri)
                .jweKey(jweKey)
                .jweAlgorithm(JWEAlgorithm.parse(jweAlgorithm))
                .jweMethod(EncryptionMethod.parse(jweMethod))
                .build();
    }

    // public JwtHttpMessageConverter(String jwksUri, JWK jwk) {
    //     super(new MediaType("application", "jwt"), new MediaType("application", "jose"));
    //     Assert.hasText(jwksUri, "jwks set uri is required");
    //     Assert.notNull(jwk, "client jwk is required");
    //         JwkSetUriJwtDecoderBuilder builder = NimbusJwtDecoder.withJwkSetUri(jwksUri);

    // jwk.getAlgorithm()

    //         //parse key
    //         JWEKeySelector jweKeySelector =
    //     new JWEDecryptionKeySelector(expectedJWEAlg, expectedJWEEnc, jweKeySource);

    //         builder.jwtProcessorCustomizer(processor -> processor.set)
    // }

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

    /**
     * A builder for creating {@link NimbusJwtDecoder} instances based on a
     * <a target="_blank" href="https://tools.ietf.org/html/rfc7517#section-5">JWK Set</a>
     * uri.
     */
    public static final class JwkSetUriJwtDecoderBuilder {

        private String jwkSetUri;

        private Set<SignatureAlgorithm> signatureAlgorithms = new HashSet<>();
        private JWK encKey;
        private JWEAlgorithm encAlgorithm = null;
        private EncryptionMethod encMethod = null;

        private RestOperations restOperations = new RestTemplate();

        private Cache cache;

        private Consumer<ConfigurableJWTProcessor<SecurityContext>> jwtProcessorCustomizer;

        private JwkSetUriJwtDecoderBuilder(String jwkSetUri) {
            Assert.hasText(jwkSetUri, "jwkSetUri cannot be empty");
            this.jwkSetUri = jwkSetUri;
            this.jwtProcessorCustomizer = processor -> {};
        }

        /**
         * Append the given signing
         * <a href="https://tools.ietf.org/html/rfc7515#section-4.1.1" target=
         * "_blank">algorithm</a> to the set of algorithms to use.
         * @param signatureAlgorithm the algorithm to use
         * @return a {@link JwkSetUriJwtDecoderBuilder} for further configurations
         */
        public JwkSetUriJwtDecoderBuilder jwsAlgorithm(SignatureAlgorithm signatureAlgorithm) {
            Assert.notNull(signatureAlgorithm, "signatureAlgorithm cannot be null");
            this.signatureAlgorithms.add(signatureAlgorithm);
            return this;
        }

        /**
         * Configure the list of
         * <a href="https://tools.ietf.org/html/rfc7515#section-4.1.1" target=
         * "_blank">algorithms</a> to use with the given {@link Consumer}.
         * @param signatureAlgorithmsConsumer a {@link Consumer} for further configuring
         * the algorithm list
         * @return a {@link JwkSetUriJwtDecoderBuilder} for further configurations
         */
        public JwkSetUriJwtDecoderBuilder jwsAlgorithms(Consumer<Set<SignatureAlgorithm>> signatureAlgorithmsConsumer) {
            Assert.notNull(signatureAlgorithmsConsumer, "signatureAlgorithmsConsumer cannot be null");
            signatureAlgorithmsConsumer.accept(this.signatureAlgorithms);
            return this;
        }

        public JwkSetUriJwtDecoderBuilder jweAlgorithm(JWEAlgorithm encAlgorithm) {
            Assert.notNull(encAlgorithm, "encAlgorithm cannot be null");
            this.encAlgorithm = encAlgorithm;
            return this;
        }

        public JwkSetUriJwtDecoderBuilder jweMethod(EncryptionMethod encMethod) {
            Assert.notNull(encMethod, "encMethod cannot be null");
            this.encMethod = encMethod;
            return this;
        }

        public JwkSetUriJwtDecoderBuilder jweKey(JWK encKey) {
            Assert.notNull(encKey, "encKey cannot be null");
            this.encKey = encKey;
            return this;
        }

        /**
         * Use the given {@link RestOperations} to coordinate with the authorization
         * servers indicated in the
         * <a href="https://tools.ietf.org/html/rfc7517#section-5">JWK Set</a> uri as well
         * as the <a href=
         * "https://openid.net/specs/openid-connect-core-1_0.html#IssuerIdentifier">Issuer</a>.
         * @param restOperations
         * @return
         */
        public JwkSetUriJwtDecoderBuilder restOperations(RestOperations restOperations) {
            Assert.notNull(restOperations, "restOperations cannot be null");
            this.restOperations = restOperations;
            return this;
        }

        /**
         * Use the given {@link Cache} to store
         * <a href="https://tools.ietf.org/html/rfc7517#section-5">JWK Set</a>.
         * @param cache the {@link Cache} to be used to store JWK Set
         * @return a {@link JwkSetUriJwtDecoderBuilder} for further configurations
         * @since 5.4
         */
        public JwkSetUriJwtDecoderBuilder cache(Cache cache) {
            Assert.notNull(cache, "cache cannot be null");
            this.cache = cache;
            return this;
        }

        /**
         * Use the given {@link Consumer} to customize the {@link JWTProcessor
         * ConfigurableJWTProcessor} before passing it to the build
         * {@link NimbusJwtDecoder}.
         * @param jwtProcessorCustomizer the callback used to alter the processor
         * @return a {@link JwkSetUriJwtDecoderBuilder} for further configurations
         * @since 5.4
         */
        public JwkSetUriJwtDecoderBuilder jwtProcessorCustomizer(
            Consumer<ConfigurableJWTProcessor<SecurityContext>> jwtProcessorCustomizer
        ) {
            Assert.notNull(jwtProcessorCustomizer, "jwtProcessorCustomizer cannot be null");
            this.jwtProcessorCustomizer = jwtProcessorCustomizer;
            return this;
        }

        JWSKeySelector<SecurityContext> jwsKeySelector(JWKSource<SecurityContext> jwkSource) {
            if (this.signatureAlgorithms.isEmpty()) {
                return new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
            }
            Set<JWSAlgorithm> jwsAlgorithms = new HashSet<>();
            for (SignatureAlgorithm signatureAlgorithm : this.signatureAlgorithms) {
                JWSAlgorithm jwsAlgorithm = JWSAlgorithm.parse(signatureAlgorithm.getName());
                jwsAlgorithms.add(jwsAlgorithm);
            }
            return new JWSVerificationKeySelector<>(jwsAlgorithms, jwkSource);
        }

        JWEKeySelector<SecurityContext> jwePrivateKeySelector(JWKSource<SecurityContext> jwkSource) {
            if (this.encAlgorithm == null || this.encMethod == null || this.encKey == null) {
                throw new IllegalArgumentException();
            }

            //use a private key from user
            return new JWEDecryptionKeySelector<>(encAlgorithm, encMethod, jwkSource);
        }

        JWEKeySelector<SecurityContext> jwePublicKeySelector(JWKSource<SecurityContext> jwkSource) {
            //use public keys from source
            return new JWEPublicKeySelector<>(jwkSource);
        }

        JWKSource<SecurityContext> jwkSource(ResourceRetriever jwkSetRetriever) {
            if (this.cache == null) {
                return new RemoteJWKSet<>(toURL(this.jwkSetUri), jwkSetRetriever);
            }
            JWKSetCache jwkSetCache = new SpringJWKSetCache(this.jwkSetUri, this.cache);
            return new RemoteJWKSet<>(toURL(this.jwkSetUri), jwkSetRetriever, jwkSetCache);
        }

        JWTProcessor<SecurityContext> processor() {
            ResourceRetriever jwkSetRetriever = new RestOperationsResourceRetriever(this.restOperations);
            JWKSource<SecurityContext> jwkSource = jwkSource(jwkSetRetriever);
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSKeySelector(jwsKeySelector(jwkSource));

            if (this.encKey != null && this.encAlgorithm != null && this.encMethod != null) {
                jwtProcessor.setJWEKeySelector(jwePrivateKeySelector(new ImmutableJWKSet<>(new JWKSet(encKey))));
            } else {
                jwtProcessor.setJWEKeySelector(jwePublicKeySelector(jwkSource));
            }

            // Spring Security validates the claim set independent from Nimbus
            jwtProcessor.setJWTClaimsSetVerifier((claims, context) -> {});
            this.jwtProcessorCustomizer.accept(jwtProcessor);
            return jwtProcessor;
        }

        /**
         * Build the configured {@link NimbusJwtDecoder}.
         * @return the configured {@link NimbusJwtDecoder}
         */
        public NimbusJwtDecoder build() {
            return new NimbusJwtDecoder(processor());
        }

        private static URL toURL(String url) {
            try {
                return new URL(url);
            } catch (MalformedURLException ex) {
                throw new IllegalArgumentException("Invalid JWK Set URL \"" + url + "\" : " + ex.getMessage(), ex);
            }
        }

        private static final class SpringJWKSetCache implements JWKSetCache {

            private final String jwkSetUri;

            private final Cache cache;

            private JWKSet jwkSet;

            SpringJWKSetCache(String jwkSetUri, Cache cache) {
                this.jwkSetUri = jwkSetUri;
                this.cache = cache;
                this.updateJwkSetFromCache();
            }

            private void updateJwkSetFromCache() {
                String cachedJwkSet = this.cache.get(this.jwkSetUri, String.class);
                if (cachedJwkSet != null) {
                    try {
                        this.jwkSet = JWKSet.parse(cachedJwkSet);
                    } catch (ParseException ignored) {
                        // Ignore invalid cache value
                    }
                }
            }

            // Note: Only called from inside a synchronized block in RemoteJWKSet.
            @Override
            public void put(JWKSet jwkSet) {
                this.jwkSet = jwkSet;
                this.cache.put(this.jwkSetUri, jwkSet.toString(false));
            }

            @Override
            public JWKSet get() {
                return (!requiresRefresh()) ? this.jwkSet : null;
            }

            @Override
            public boolean requiresRefresh() {
                return this.cache.get(this.jwkSetUri) == null;
            }
        }

        private static class RestOperationsResourceRetriever implements ResourceRetriever {

            private static final MediaType APPLICATION_JWK_SET_JSON = new MediaType("application", "jwk-set+json");

            private final RestOperations restOperations;

            RestOperationsResourceRetriever(RestOperations restOperations) {
                Assert.notNull(restOperations, "restOperations cannot be null");
                this.restOperations = restOperations;
            }

            @Override
            public Resource retrieveResource(URL url) throws IOException {
                HttpHeaders headers = new HttpHeaders();
                headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, APPLICATION_JWK_SET_JSON));
                ResponseEntity<String> response = getResponse(url, headers);
                if (response.getStatusCodeValue() != 200) {
                    throw new IOException(response.toString());
                }
                return new Resource(response.getBody(), "UTF-8");
            }

            private ResponseEntity<String> getResponse(URL url, HttpHeaders headers) throws IOException {
                try {
                    RequestEntity<Void> request = new RequestEntity<>(headers, HttpMethod.GET, url.toURI());
                    return this.restOperations.exchange(request, String.class);
                } catch (Exception ex) {
                    throw new IOException(ex);
                }
            }
        }
    }
}
