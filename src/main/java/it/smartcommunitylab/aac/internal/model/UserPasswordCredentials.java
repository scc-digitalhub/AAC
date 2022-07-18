package it.smartcommunitylab.aac.internal.model;

import javax.validation.Valid;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractUserCredentials;

@Valid
public class UserPasswordCredentials extends AbstractUserCredentials {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    public UserPasswordCredentials(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, userId);
        Assert.hasText(userId, "userId can not be null or blank");
    }

    private String password;
    private Boolean changeOnFirstAccess;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getChangeOnFirstAccess() {
        return changeOnFirstAccess;
    }

    public void setChangeOnFirstAccess(Boolean changeOnFirstAccess) {
        this.changeOnFirstAccess = changeOnFirstAccess;
    }

    public boolean isChangeOnFirstAccess() {
        return changeOnFirstAccess != null ? changeOnFirstAccess.booleanValue() : false;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS + "_" + CredentialsType.PASSWORD.getValue();
    }

    @Override
    @JsonIgnore
    public String getCredentials() {
        return password;
    }

    @Override
    public String getId() {
        return getUserId() + ":password";
    }

    @Override
    public String getUuid() {
        return null;
    }

}
