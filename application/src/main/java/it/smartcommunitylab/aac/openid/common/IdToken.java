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

package it.smartcommunitylab.aac.openid.common;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import it.smartcommunitylab.aac.SystemKeys;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.util.Assert;

public class IdToken extends OidcIdToken {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    public static final Set<String> REGISTERED_CLAIM_NAMES = JWTClaimsSet.getRegisteredNames();
    public static final Set<String> STANDARD_CLAIM_NAMES = IDTokenClaimsSet.getStandardClaimNames();

    private final String issuer;
    private final String subject;
    private final List<String> audience;
    private final Instant notBefore;

    // value is set to the result (ie JWT)
    private String value;

    public IdToken(
        String issuer,
        String subject,
        String[] audience,
        String tokenValue,
        Instant issuedAt,
        Instant expiresAt,
        Instant notBefore,
        Map<String, Object> claims
    ) {
        super(tokenValue, issuedAt, expiresAt, claims);
        Assert.hasText(issuer, "issuer can not be null or empty");
        Assert.hasText(subject, "subject can not be null or empty");

        this.issuer = issuer;
        this.subject = subject;
        this.notBefore = notBefore;

        if (audience != null) {
            this.audience = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(audience)));
        } else {
            this.audience = Collections.emptyList();
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSubject() {
        return subject;
    }

    public Instant getNotBefore() {
        return notBefore;
    }

    public List<String> getAudience() {
        return audience;
    }

    public JWTClaimsSet getClaimsSet() {
        // build claims set according to OIDC 1.0
        JWTClaimsSet.Builder idClaims = new JWTClaimsSet.Builder();

        // set all claims from map, then set reserved
        Map<String, Object> claims = getClaims();
        claims
            .entrySet()
            .stream()
            .filter(c -> !REGISTERED_CLAIM_NAMES.contains(c.getKey()))
            .forEach(c -> {
                idClaims.claim(c.getKey(), c.getValue());
            });

        // reserved claims
        idClaims.issuer(issuer);
        idClaims.subject(subject);
        idClaims.jwtID(getTokenValue());
        idClaims.audience(audience);

        Date iat = new Date();
        if (getIssuedAt() != null) {
            iat = Date.from(getIssuedAt());
        }
        idClaims.issueTime(iat);

        Date nbf = iat;
        if (getNotBefore() != null) {
            nbf = Date.from(getNotBefore());
        }
        idClaims.notBeforeTime(nbf);

        if (getExpiresAt() != null) {
            idClaims.expirationTime(Date.from(getExpiresAt()));
        }

        return idClaims.build();
    }
}
