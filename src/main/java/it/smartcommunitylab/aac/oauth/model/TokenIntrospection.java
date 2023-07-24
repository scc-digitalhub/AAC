/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.oauth.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.smartcommunitylab.aac.repository.StringArraySerializer;
import it.smartcommunitylab.aac.repository.StringOrArraySerializer;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * IETF RFC7662 Token Introspection model
 *
 * @author raman
 *
 */
@JsonInclude(Include.NON_NULL)
public class TokenIntrospection {

    private static final Set<String> RESERVED_CLAIM_NAMES;

    static {
        Set<String> n = new HashSet<>();

        n.add("active");
        n.add("jti");
        n.add("scope");
        n.add("client_id");
        n.add("token_type");
        n.add("exp");
        n.add("iat");
        n.add("nbf");
        n.add("sub");
        n.add("iss");
        n.add("aud");
        n.add("azp");
        // support access_token as jwt/opaque
        n.add("access_token");

        RESERVED_CLAIM_NAMES = Collections.unmodifiableSet(n);
    }

    private final boolean active;

    @JsonProperty("jti")
    private String jti;

    @JsonProperty("scope")
    @JsonSerialize(using = StringArraySerializer.class)
    private Set<String> scope;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("exp")
    private Integer expirationTime;

    @JsonProperty("iat")
    private Integer issuedAt;

    @JsonProperty("nbf")
    private Integer notBeforeTime;

    @JsonProperty("sub")
    private String subject;

    @JsonProperty("iss")
    private String issuer;

    @JsonProperty("aud")
    @JsonSerialize(using = StringOrArraySerializer.class)
    private Set<String> audience;

    @JsonProperty("azp")
    private String authorizedParty;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonIgnore
    Map<String, Serializable> claims;

    public TokenIntrospection(boolean active) {
        this.active = active;
        this.claims = new HashMap<>();
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Set<String> getScope() {
        return scope;
    }

    public void setScope(Set<String> scope) {
        this.scope = scope;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Integer getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Integer expirationTime) {
        this.expirationTime = expirationTime;
    }

    public Integer getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Integer issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Integer getNotBeforeTime() {
        return notBeforeTime;
    }

    public void setNotBeforeTime(Integer notBeforeTime) {
        this.notBeforeTime = notBeforeTime;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Set<String> getAudience() {
        return audience;
    }

    public void setAudience(Collection<String> audience) {
        this.audience = new HashSet<>(audience);
    }

    public void setAudience(String audience) {
        this.audience = Collections.singleton(audience);
    }

    public String getAuthorizedParty() {
        return authorizedParty;
    }

    public void setAuthorizedParty(String authorizedParty) {
        this.authorizedParty = authorizedParty;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public boolean isActive() {
        return active;
    }

    public Map<String, Serializable> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, Serializable> claims) {
        this.claims =
            claims
                .entrySet()
                .stream()
                .filter(c -> !RESERVED_CLAIM_NAMES.contains(c.getKey()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    public void addClaim(String key, Serializable value) {
        if (RESERVED_CLAIM_NAMES.contains(key)) {
            throw new IllegalArgumentException("can't set a reserved claim");
        }
        claims.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Serializable> getMap() {
        return claims;
    }
}
