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

import com.nimbusds.jwt.JWTClaimNames;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class JwtClientAuthAssertionTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final Duration DEFAULT_CLOCK_SKEW = Duration.ofSeconds(60);
    private static final Duration DEFAULT_MAX_VALIDITY = Duration.ofSeconds(300);

    private Duration clockSkew = DEFAULT_CLOCK_SKEW;
    private Duration maxValidity = DEFAULT_MAX_VALIDITY;

    private final OAuth2ClientDetails clientDetails;
    private final Set<String> audience;

    public JwtClientAuthAssertionTokenValidator(OAuth2ClientDetails clientDetails, String... audience) {
        this(clientDetails, Arrays.asList(audience));
    }

    public JwtClientAuthAssertionTokenValidator(OAuth2ClientDetails clientDetails, Collection<String> audience) {
        Assert.notNull(clientDetails, "client details can not be null");
        Assert.notEmpty(audience, "audience can not be null");
        this.clientDetails = clientDetails;
        this.audience = new HashSet<>(audience);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        /*
         * JWT validation according to
         *
         * https://datatracker.ietf.org/doc/html/rfc7523#section-3
         */

        // validate REQUIRED claims
        Set<String> invalidClaims = Arrays
            .asList(JWT_REQUIRED_ATTRIBUTES)
            .stream()
            .filter(c -> !token.hasClaim(c))
            .collect(Collectors.toSet());
        if (!invalidClaims.isEmpty()) {
            return OAuth2TokenValidatorResult.failure(buildError(token, invalidClaims));
        }

        invalidClaims = new HashSet<>();

        // 1. issuer is the client
        String issuer = token.getClaimAsString(IdTokenClaimNames.ISS);
        if (issuer == null || !clientDetails.getClientId().equals(issuer)) {
            invalidClaims.add(IdTokenClaimNames.ISS);
        }

        // 2. subject is the clientId
        String subject = token.getSubject();
        if (subject == null || !clientDetails.getClientId().equals(subject)) {
            invalidClaims.add(IdTokenClaimNames.SUB);
        }

        // 3. aud must contain the authorization server identifier
        // check that at least one is present
        String aud = token.getAudience().stream().filter(a -> audience.contains(a)).findFirst().orElse(null);
        if (aud == null) {
            invalidClaims.add(IdTokenClaimNames.AUD);
        }

        // 4. exp time in current window
        Instant now = Instant.now();
        if (now.minus(this.clockSkew).isAfter(token.getExpiresAt())) {
            invalidClaims.add(IdTokenClaimNames.EXP);
        }

        // validate OPT claims

        // 5. not before in current window
        if (token.hasClaim(JWTClaimNames.NOT_BEFORE)) {
            if (now.minus(this.clockSkew).isAfter(token.getNotBefore())) {
                invalidClaims.add(JWTClaimNames.NOT_BEFORE);
            }
        }

        // 6. issued at in the past
        Instant iat = now.minus(clockSkew);
        if (token.hasClaim(JWTClaimNames.ISSUED_AT) && token.getIssuedAt() != null) {
            if (now.plus(this.clockSkew).isBefore(token.getIssuedAt())) {
                invalidClaims.add(JWTClaimNames.ISSUED_AT);
            }

            // also validate max validity against exp
            iat = token.getIssuedAt();

            if (iat.plus(this.maxValidity).isBefore(token.getExpiresAt())) {
                invalidClaims.add(JWTClaimNames.ISSUED_AT);
            }
        }

        // 7. jti identifier to check replay
        // TODO evaluate, not supported
        // for now check that id is not null
        // RFC7523 says field is OPTIONAL but OpenID says REQUIRED
        // https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication
        String jti = token.getClaimAsString(JWTClaimNames.JWT_ID);
        if (jti == null || !StringUtils.hasText(jti)) {
            invalidClaims.add(JWTClaimNames.JWT_ID);
        }

        if (!invalidClaims.isEmpty()) {
            return OAuth2TokenValidatorResult.failure(buildError(token, invalidClaims));
        }
        return OAuth2TokenValidatorResult.success();
    }

    public void setClockSkew(Duration clockSkew) {
        Assert.notNull(clockSkew, "clockSkew cannot be null");
        Assert.isTrue(clockSkew.getSeconds() >= 0, "clockSkew must be >= 0");
        this.clockSkew = clockSkew;
    }

    public void setMaxValidity(Duration maxValidity) {
        Assert.notNull(maxValidity, "maxValidity cannot be null");
        Assert.isTrue(
            maxValidity.getSeconds() >= this.clockSkew.getSeconds(),
            "maxValidity must be bigger than clockSkew"
        );
        this.maxValidity = maxValidity;
    }

    private static OAuth2Error buildError(Jwt token, Collection<String> invalidClaims) {
        return new OAuth2Error(
            "invalid_client",
            "The client JWT contains invalid claims: " + invalidClaims,
            "https://datatracker.ietf.org/doc/html/rfc7523#section-3"
        );
    }

    public static String[] JWT_REQUIRED_ATTRIBUTES = {
        IdTokenClaimNames.ISS,
        IdTokenClaimNames.SUB,
        IdTokenClaimNames.AUD,
        IdTokenClaimNames.EXP,
    };

    public static String[] JWT_OPTIONAL_ATTRIBUTES = {
        JWTClaimNames.NOT_BEFORE,
        JWTClaimNames.ISSUED_AT,
        JWTClaimNames.JWT_ID,
    };
}
