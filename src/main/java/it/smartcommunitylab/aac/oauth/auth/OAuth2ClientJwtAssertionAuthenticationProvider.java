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

package it.smartcommunitylab.aac.oauth.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import it.smartcommunitylab.aac.clients.auth.ClientAuthentication;
import it.smartcommunitylab.aac.clients.auth.ClientAuthenticationProvider;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OAuth2ClientJwtAssertionAuthenticationProvider
    extends ClientAuthenticationProvider
    implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Collection<String> audience;
    private final OAuth2ClientDetailsService clientDetailsService;

    public OAuth2ClientJwtAssertionAuthenticationProvider(
        OAuth2ClientDetailsService clientDetailsService,
        String... audience
    ) {
        this(clientDetailsService, Arrays.asList(audience));
    }

    public OAuth2ClientJwtAssertionAuthenticationProvider(
        OAuth2ClientDetailsService clientDetailsService,
        Collection<String> audience
    ) {
        Assert.notNull(clientDetailsService, "client details service is required");
        Assert.notEmpty(audience, "audience is required to validate jwt assertions");

        this.audience = audience;
        this.clientDetailsService = clientDetailsService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(clientService, "client service is required");
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(
            OAuth2ClientJwtAssertionAuthenticationToken.class,
            authentication,
            "Only OAuth2ClientJwtAssertionAuthenticationToken is supported"
        );

        OAuth2ClientJwtAssertionAuthenticationToken authRequest =
            (OAuth2ClientJwtAssertionAuthenticationToken) authentication;
        String clientId = authRequest.getPrincipal();
        String clientAssertion = authRequest.getClientAssertion();

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientAssertion)) {
            throw new BadCredentialsException("missing required parameters in request");
        }

        try {
            // check if auth method is undefined and resolve from jwt
            AuthenticationMethod authMethod = null;
            String authenticationMethod = null;
            JWT jwt = JWTParser.parse(clientAssertion);
            // rsa requires private key
            if (JWSAlgorithm.Family.RSA.contains(jwt.getHeader().getAlgorithm())) {
                authMethod = AuthenticationMethod.PRIVATE_KEY_JWT;
            } else {
                // fallback to shared secret
                authMethod = AuthenticationMethod.CLIENT_SECRET_JWT;
            }
            authenticationMethod = authMethod.getValue();

            // load details, we need to check request
            OAuth2ClientDetails client = clientDetailsService.loadClientByClientId(clientId);

            // check if client can authenticate with this scheme
            if (!client.getAuthenticationMethods().contains(authenticationMethod)) {
                this.logger.debug("Failed to authenticate since client can not use scheme " + authenticationMethod);
                throw new BadCredentialsException("invalid authentication");
            }

            // load jwt decoder according to auth method
            NimbusJwtDecoder jwtDecoder = null;
            if (AuthenticationMethod.CLIENT_SECRET_JWT == authMethod) {
                // build key from secret as HmacSHA256
                SecretKeySpec secretKey = new SecretKeySpec(
                    client.getClientSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
                );

                jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
            }
            if (AuthenticationMethod.PRIVATE_KEY_JWT == authMethod) {
                // load first RSA key from jwks
                JWKSet jwks = JWKSet.parse(client.getJwks());
                JWK jwk = jwks
                    .getKeys()
                    .stream()
                    .filter(k -> JWSAlgorithm.Family.RSA.contains(k.getAlgorithm()))
                    .findFirst()
                    .orElse(null);
                if (jwk == null) {
                    this.logger.debug("Failed to authenticate since jwks does not contain a valid key");
                    throw new BadCredentialsException("invalid authentication");
                }

                RSAPublicKey publicKey = jwk.toRSAKey().toRSAPublicKey();
                jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
            }

            if (jwtDecoder == null) {
                this.logger.debug(
                        "Failed to authenticate since auth method is unsupported: " +
                        String.valueOf(authenticationMethod)
                    );
                throw new BadCredentialsException("invalid authentication");
            }

            /*
             * We authenticate by evaluating the assertion
             */
            JwtClientAuthAssertionTokenValidator validator = new JwtClientAuthAssertionTokenValidator(client, audience);
            Jwt assertion = jwtDecoder.decode(clientAssertion);
            OAuth2TokenValidatorResult assertResult = validator.validate(assertion);
            if (assertResult.hasErrors()) {
                this.logger.debug(
                        "Failed to authenticate due to assertion errors: " + String.valueOf(assertResult.getErrors())
                    );
                throw new BadCredentialsException("invalid authentication");
            }

            // load authorities from clientService
            Collection<GrantedAuthority> authorities;
            try {
                ClientDetails clientDetails = clientService.loadClient(clientId);
                authorities = clientDetails.getAuthorities();
            } catch (NoSuchClientException e) {
                throw new ClientRegistrationException("invalid client");
            }

            // result contains credentials, someone later on will need to call
            // eraseCredentials
            OAuth2ClientJwtAssertionAuthenticationToken result = new OAuth2ClientJwtAssertionAuthenticationToken(
                clientId,
                clientAssertion,
                authorities
            );

            // save details
            // TODO add ClientDetails in addition to oauth2ClientDetails
            result.setOAuth2ClientDetails(client);
            result.setWebAuthenticationDetails(authRequest.getWebAuthenticationDetails());

            return result;
        } catch (ClientRegistrationException | ParseException | JOSEException | JwtException e) {
            throw new BadCredentialsException("invalid authentication");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (OAuth2ClientJwtAssertionAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
