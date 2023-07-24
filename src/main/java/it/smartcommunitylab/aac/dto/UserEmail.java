package it.smartcommunitylab.aac.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.Valid;
import javax.validation.constraints.Email;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserEmail {

    @Email(message = "{validation.email}")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
