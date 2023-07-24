package it.smartcommunitylab.aac.webauthn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import org.springframework.util.Assert;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnRegistrationStartRequest {

    @NotBlank
    private String username;

    private String displayName;

    public WebAuthnRegistrationStartRequest() {}

    public WebAuthnRegistrationStartRequest(String username, String displayName) {
        Assert.hasText(username, "username can not be null or blank");
        this.username = username;
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
