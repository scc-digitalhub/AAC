package it.smartcommunitylab.aac.password.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Valid
public class UserPasswordBean {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String verifyPassword;

    private String curPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVerifyPassword() {
        return verifyPassword;
    }

    public void setVerifyPassword(String verifyPassword) {
        this.verifyPassword = verifyPassword;
    }

    public String getCurPassword() {
        return curPassword;
    }

    public void setCurPassword(String curPassword) {
        this.curPassword = curPassword;
    }

}
