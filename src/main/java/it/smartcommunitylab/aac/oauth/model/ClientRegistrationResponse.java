package it.smartcommunitylab.aac.oauth.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@JsonInclude(Include.NON_NULL)
public class ClientRegistrationResponse {

    @JsonUnwrapped
    private final ClientRegistration registration;

    @JsonProperty("registration_client_uri")
    private String registrationUri;

    @JsonProperty("registration_access_token")
    private String registrationToken;

    public ClientRegistrationResponse(ClientRegistration registration) {
        Assert.notNull(registration, "registration can not be null");
        this.registration = registration;
    }

    public ClientRegistration getRegistration() {
        return registration;
    }

    public String getRegistrationUri() {
        return registrationUri;
    }

    public void setRegistrationUri(String registrationUri) {
        this.registrationUri = registrationUri;
    }

    public String getRegistrationToken() {
        return registrationToken;
    }

    public void setRegistrationToken(String registrationToken) {
        this.registrationToken = registrationToken;
    }

}
