package it.smartcommunitylab.aac.oauth.model;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import it.smartcommunitylab.aac.repository.StringArraySerializer;

@JsonInclude(Include.NON_NULL)
public class AuthorizationResponse {

    @JsonProperty("code")
    private String code;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("id_token")
    private String idToken;

    @JsonProperty("scope")
    @JsonSerialize(using = StringArraySerializer.class)
    private Set<String> scope;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("state")
    private String state;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    @JsonProperty("iss")
    private String issuer;

    @JsonIgnore
    private Map<String, Serializable> additionalInformation;

    public AuthorizationResponse() {
    }

    public AuthorizationResponse(String code) {
        Assert.notNull(code, "code can not be null");
        this.code = code;
    }

    public AuthorizationResponse(OAuth2AccessToken accessToken) {
        Assert.notNull(accessToken, "access token can not be null");
        this.tokenType = accessToken.getTokenType();
        this.accessToken = accessToken.getValue();
        this.scope = accessToken.getScope();
        this.expiresIn = accessToken.getExpiresIn();

        if (accessToken.getAdditionalInformation() != null) {
            // keep only serializable properties
            this.additionalInformation = accessToken.getAdditionalInformation().entrySet().stream()
                    .filter(e -> (e.getValue() instanceof Serializable))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> (Serializable) e.getValue()));
        }

    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public Set<String> getScope() {
        return scope;
    }

    public void setScope(Set<String> scope) {
        this.scope = scope;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
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
