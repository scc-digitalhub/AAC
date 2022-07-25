package it.smartcommunitylab.aac.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import it.smartcommunitylab.aac.model.UserStatus;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserStatusBean {

    @NotNull
    private UserStatus status;

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

}
