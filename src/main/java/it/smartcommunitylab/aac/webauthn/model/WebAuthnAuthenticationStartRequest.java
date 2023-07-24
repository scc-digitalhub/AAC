package it.smartcommunitylab.aac.webauthn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnAuthenticationStartRequest {

    @JsonProperty("username")
    @NotBlank
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
