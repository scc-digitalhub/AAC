package it.smartcommunitylab.aac.dto;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
