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

package it.smartcommunitylab.aac.oauth.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.smartcommunitylab.aac.repository.StringArraySerializer;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.Assert;

@JsonInclude(Include.NON_NULL)
public class TokenResponse {

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("id_token")
    private String idToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    @JsonProperty("scope")
    @JsonSerialize(using = StringArraySerializer.class)
    private Set<String> scope;

    @JsonIgnore
    private Map<String, Serializable> additionalInformation;

    public TokenResponse() {
        this.scope = Collections.emptySet();
    }

    public TokenResponse(OAuth2AccessToken accessToken) {
        Assert.notNull(accessToken, "access token can not be null");
        this.tokenType = accessToken.getTokenType();
        this.accessToken = accessToken.getValue();
        this.scope = accessToken.getScope();
        this.expiresIn = accessToken.getExpiresIn();
        if (accessToken.getRefreshToken() != null) {
            this.refreshToken = accessToken.getRefreshToken().getValue();
        }

        if (accessToken.getAdditionalInformation() != null) {
            // keep only serializable properties
            this.additionalInformation = accessToken
                .getAdditionalInformation()
                .entrySet()
                .stream()
                .filter(e -> (e.getValue() instanceof Serializable))
                .collect(Collectors.toMap(e -> e.getKey(), e -> (Serializable) e.getValue()));
        }
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Set<String> getScope() {
        return scope;
    }

    public void setScope(Set<String> scope) {
        if (scope == null) {
            // as per spec, scope is OPTIONAL if identical to request, otherwise REQUIRED.
            // Since we don't have access to request here we always add in response
            this.scope = Collections.emptySet();
        }

        this.scope = scope;
    }

    public Map<String, Serializable> getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(Map<String, Serializable> additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    @JsonAnyGetter
    public Map<String, Serializable> getMap() {
        return additionalInformation;
    }
}
