package it.smartcommunitylab.aac.webauthn.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnAuthenticationStartRequest {

    @JsonProperty("username")
    @NotNull
    private String username;
 
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    } 
}
