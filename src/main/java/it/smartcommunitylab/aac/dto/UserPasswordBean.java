package it.smartcommunitylab.aac.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Valid
public class UserPasswordBean {

    @NotBlank
    private String userId;

    @NotBlank
    private String password;

    @NotBlank
    private String verifyPassword;

    private String curPassword;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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
