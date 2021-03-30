package it.smartcommunitylab.aac.oauth.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.smartcommunitylab.aac.core.model.ClientCredentials;

public class ClientSecret implements ClientCredentials {

    private String clientId;
    private final String secret;

    public ClientSecret(@JsonProperty("clientSecret") String secret) {
        Assert.notNull(secret, "secret can not be null");
        this.secret = secret;
    }

    public ClientSecret(String clientId, String secret) {
        Assert.notNull(secret, "secret can not be null");
        this.clientId = clientId;
        this.secret = secret;
    }

    @Override
    @JsonIgnore
    public String getCredentials() {
        return secret;
    }

    public String getClientSecret() {
        return secret;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

}
