package it.smartcommunitylab.aac.dto;

import javax.validation.constraints.Email;

public class UserResetBean {

    @Email(message = "{validation.email}")
    private String email;

    private String username;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
