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

package it.smartcommunitylab.aac.oidc.auth;

import it.smartcommunitylab.aac.jwt.JwtDecoderBuilder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minidev.json.JSONObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.converter.ClaimTypeConverter;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/*
 * IdToken decoder factory supporting JWKSet via uri or provided as metadata
 */

public class OIDCIdTokenDecoderFactory implements JwtDecoderFactory<ClientRegistration> {

    private static final String MISSING_SIGNATURE_VERIFIER_ERROR_CODE = "missing_signature_verifier";

    private static final ClaimTypeConverter DEFAULT_CLAIM_TYPE_CONVERTER = new ClaimTypeConverter(
        OidcIdTokenDecoderFactory.createDefaultClaimTypeConverters()
    );

    private final Map<String, JwtDecoder> jwtDecoders = new ConcurrentHashMap<>();

    private Function<ClientRegistration, OAuth2TokenValidator<Jwt>> jwtValidatorFactory = clientRegistration ->
        new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), new OidcIdTokenValidator(clientRegistration));

    private Function<ClientRegistration, JwsAlgorithm> jwsAlgorithmResolver = clientRegistration ->
        SignatureAlgorithm.RS256;

    private Function<ClientRegistration, Converter<Map<String, Object>, Map<String, Object>>> claimTypeConverterFactory =
        clientRegistration -> DEFAULT_CLAIM_TYPE_CONVERTER;

    @Override
    public JwtDecoder createDecoder(ClientRegistration clientRegistration) {
        Assert.notNull(clientRegistration, "clientRegistration cannot be null");
        return this.jwtDecoders.computeIfAbsent(
                clientRegistration.getRegistrationId(),
                key -> {
                    NimbusJwtDecoder jwtDecoder = buildDecoder(clientRegistration);
                    jwtDecoder.setJwtValidator(this.jwtValidatorFactory.apply(clientRegistration));
                    Converter<Map<String, Object>, Map<String, Object>> claimTypeConverter =
                        this.claimTypeConverterFactory.apply(clientRegistration);
                    if (claimTypeConverter != null) {
                        jwtDecoder.setClaimSetConverter(claimTypeConverter);
                    }
                    return jwtDecoder;
                }
            );
    }

    private NimbusJwtDecoder buildDecoder(ClientRegistration clientRegistration) {
        JwsAlgorithm jwsAlgorithm = this.jwsAlgorithmResolver.apply(clientRegistration);
        if (jwsAlgorithm != null && SignatureAlgorithm.class.isAssignableFrom(jwsAlgorithm.getClass())) {
            // https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation
            //
            // 6. If the ID Token is received via direct communication between the Client
            // and the Token Endpoint (which it is in this flow),
            // the TLS server validation MAY be used to validate the issuer in place of
            // checking the token signature.
            // The Client MUST validate the signature of all other ID Tokens according to
            // JWS [JWS]
            // using the algorithm specified in the JWT alg Header Parameter.
            // The Client MUST use the keys provided by the Issuer.
            //
            // 7. The alg value SHOULD be the default of RS256 or the algorithm sent by
            // the Client
            // in the id_token_signed_response_alg parameter during Registration.
            //TODO

            String jwkSetUri = clientRegistration.getProviderDetails().getJwkSetUri();
            if (StringUtils.hasText(jwkSetUri)) {
                return new JwtDecoderBuilder()
                    .jwksUri(jwkSetUri)
                    .jwsAlgorithm((SignatureAlgorithm) jwsAlgorithm)
                    .build();
            }

            //try to parse jwks from metadata
            Object jwks = clientRegistration.getProviderDetails().getConfigurationMetadata().get("jwks");
            if (jwks instanceof JSONObject) {
                return new JwtDecoderBuilder()
                    .jwks((JSONObject) jwks)
                    .jwsAlgorithm((SignatureAlgorithm) jwsAlgorithm)
                    .build();
            }
        }

        OAuth2Error oauth2Error = new OAuth2Error(
            MISSING_SIGNATURE_VERIFIER_ERROR_CODE,
            "Failed to find a Signature Verifier for Client Registration: '" +
            clientRegistration.getRegistrationId() +
            "'. Check to ensure you have configured a valid JWS Algorithm: '" +
            jwsAlgorithm +
            "'",
            null
        );
        throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
    }

    /**
     * Sets the factory that provides an {@link OAuth2TokenValidator}, which is used by
     * the {@link JwtDecoder}. The default composes {@link JwtTimestampValidator} and
     * {@link OidcIdTokenValidator}.
     * @param jwtValidatorFactory the factory that provides an
     * {@link OAuth2TokenValidator}
     */
    public void setJwtValidatorFactory(Function<ClientRegistration, OAuth2TokenValidator<Jwt>> jwtValidatorFactory) {
        Assert.notNull(jwtValidatorFactory, "jwtValidatorFactory cannot be null");
        this.jwtValidatorFactory = jwtValidatorFactory;
    }

    /**
     * Sets the resolver that provides the expected {@link JwsAlgorithm JWS algorithm}
     * used for the signature or MAC on the {@link OidcIdToken ID Token}. The default
     * resolves to {@link SignatureAlgorithm#RS256 RS256} for all
     * {@link ClientRegistration clients}.
     * @param jwsAlgorithmResolver the resolver that provides the expected
     * {@link JwsAlgorithm JWS algorithm} for a specific {@link ClientRegistration client}
     */
    public void setJwsAlgorithmResolver(Function<ClientRegistration, JwsAlgorithm> jwsAlgorithmResolver) {
        Assert.notNull(jwsAlgorithmResolver, "jwsAlgorithmResolver cannot be null");
        this.jwsAlgorithmResolver = jwsAlgorithmResolver;
    }

    /**
     * Sets the factory that provides a {@link Converter} used for type conversion of
     * claim values for an {@link OidcIdToken}. The default is {@link ClaimTypeConverter}
     * for all {@link ClientRegistration clients}.
     * @param claimTypeConverterFactory the factory that provides a {@link Converter} used
     * for type conversion of claim values for a specific {@link ClientRegistration
     * client}
     */
    public void setClaimTypeConverterFactory(
        Function<ClientRegistration, Converter<Map<String, Object>, Map<String, Object>>> claimTypeConverterFactory
    ) {
        Assert.notNull(claimTypeConverterFactory, "claimTypeConverterFactory cannot be null");
        this.claimTypeConverterFactory = claimTypeConverterFactory;
    }
}
