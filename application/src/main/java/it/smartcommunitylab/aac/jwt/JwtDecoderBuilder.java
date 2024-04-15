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

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minidev.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * Builder for a Nimbus jwt decoder - supports signing and encryption with jwks, jwksUri
 */
public final class JwtDecoderBuilder {

    //signature
    private URL jwkSetUri;
    private JWKSet jwks;
    private Set<SignatureAlgorithm> signatureAlgorithms = new HashSet<>();

    //encryption
    private JWK encKey;
    private JWEAlgorithm encAlgorithm = null;
    private EncryptionMethod encMethod = null;

    //customizable rest operations
    private RestOperations restOperations = new RestTemplate();

    //additional customizer
    private Consumer<ConfigurableJWTProcessor<SecurityContext>> jwtProcessorCustomizer;

    public JwtDecoderBuilder() {
        //defaults to no-op
        this.jwtProcessorCustomizer = processor -> {};
    }

    /*
     * JWS configuration
     */
    public JwtDecoderBuilder jwksUri(String jwkSetUri) {
        Assert.hasText(jwkSetUri, "jwkSetUri cannot be empty");
        try {
            this.jwkSetUri = new URL(jwkSetUri);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Invalid JWKSet URL: " + ex.getMessage(), ex);
        }

        return this;
    }

    public JwtDecoderBuilder jwsAlgorithm(SignatureAlgorithm signatureAlgorithm) {
        Assert.notNull(signatureAlgorithm, "signatureAlgorithm cannot be null");
        this.signatureAlgorithms.add(signatureAlgorithm);
        return this;
    }

    public JwtDecoderBuilder jwsAlgorithms(Consumer<Set<SignatureAlgorithm>> signatureAlgorithmsConsumer) {
        Assert.notNull(signatureAlgorithmsConsumer, "signatureAlgorithmsConsumer cannot be null");
        signatureAlgorithmsConsumer.accept(this.signatureAlgorithms);
        return this;
    }

    public JwtDecoderBuilder jwks(JWKSet jwks) {
        Assert.notNull(jwks, "jwks cannot be null");
        this.jwks = jwks;
        return this;
    }

    public JwtDecoderBuilder jwks(JSONObject jwks) {
        Assert.notNull(jwks, "jwks cannot be null");
        try {
            this.jwks = JWKSet.parse(jwks);
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Invalid JWKSet object: " + ex.getMessage(), ex);
        }

        return this;
    }

    public JwtDecoderBuilder jwks(String jwks) {
        Assert.hasText(jwks, "jwks cannot be null");
        try {
            this.jwks = JWKSet.parse(jwks);
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Invalid JWKSet object: " + ex.getMessage(), ex);
        }

        return this;
    }

    /*
     * JWE configuration
     */

    public JwtDecoderBuilder jweAlgorithm(JWEAlgorithm encAlgorithm) {
        Assert.notNull(encAlgorithm, "encAlgorithm cannot be null");
        this.encAlgorithm = encAlgorithm;
        return this;
    }

    public JwtDecoderBuilder jweMethod(EncryptionMethod encMethod) {
        Assert.notNull(encMethod, "encMethod cannot be null");
        this.encMethod = encMethod;
        return this;
    }

    public JwtDecoderBuilder jweKey(JWK encKey) {
        Assert.notNull(encKey, "encKey cannot be null");
        this.encKey = encKey;
        return this;
    }

    /*
     * Customization
     */
    public JwtDecoderBuilder restOperations(RestOperations restOperations) {
        Assert.notNull(restOperations, "restOperations cannot be null");
        this.restOperations = restOperations;
        return this;
    }

    public JwtDecoderBuilder jwtProcessorCustomizer(
        Consumer<ConfigurableJWTProcessor<SecurityContext>> jwtProcessorCustomizer
    ) {
        Assert.notNull(jwtProcessorCustomizer, "jwtProcessorCustomizer cannot be null");
        this.jwtProcessorCustomizer = jwtProcessorCustomizer;
        return this;
    }

    /*
     * Builder
     */

    JWSKeySelector<SecurityContext> jwsKeySelector(JWKSource<SecurityContext> jwkSource) {
        if (this.signatureAlgorithms.isEmpty()) {
            //default to RS256, support is mandatory
            return new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
        }

        Set<JWSAlgorithm> jwsAlgorithms = signatureAlgorithms
            .stream()
            .map(a -> JWSAlgorithm.parse(a.getName()))
            .collect(Collectors.toSet());

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

    JWTProcessor<SecurityContext> processor() {
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

        if (this.jwkSetUri != null) {
            JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(
                (this.jwkSetUri),
                new RestOperationsResourceRetriever(this.restOperations)
            );
            jwtProcessor.setJWSKeySelector(jwsKeySelector(jwkSource));
            jwtProcessor.setJWEKeySelector(jwePublicKeySelector(jwkSource));
        }

        if (this.jwks != null) {
            JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(this.jwks);
            jwtProcessor.setJWSKeySelector(jwsKeySelector(jwkSource));
            jwtProcessor.setJWEKeySelector(jwePublicKeySelector(jwkSource));
        }

        if (this.encKey != null && this.encAlgorithm != null && this.encMethod != null) {
            jwtProcessor.setJWEKeySelector(jwePrivateKeySelector(new ImmutableJWKSet<>(new JWKSet(encKey))));
        }

        // Spring Security validates the claim set independent from Nimbus
        jwtProcessor.setJWTClaimsSetVerifier((claims, context) -> {});
        this.jwtProcessorCustomizer.accept(jwtProcessor);
        return jwtProcessor;
    }

    public NimbusJwtDecoder build() {
        return new NimbusJwtDecoder(processor());
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
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, APPLICATION_JWK_SET_JSON));

                RequestEntity<Void> request = new RequestEntity<>(headers, HttpMethod.GET, url.toURI());
                ResponseEntity<String> response = this.restOperations.exchange(request, String.class);

                if (response.getStatusCodeValue() != 200) {
                    throw new IOException(response.toString());
                }

                return new Resource(response.getBody(), "UTF-8");
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }
    }
}
