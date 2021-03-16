package it.smartcommunitylab.aac.oauth.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.smartcommunitylab.aac.core.model.ClientCredentials;

public class ClientSecret implements ClientCredentials {

    private final String secret;

    public ClientSecret(@JsonProperty("clientSecret") String secret) {
        Assert.notNull(secret, "secret can not be null");
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

}
