package it.smartcommunitylab.aac.password.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractEditableUserCredentials;

@Valid
@JsonInclude(Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalEditableUserPassword extends AbstractEditableUserCredentials {
    private static final long serialVersionUID = SystemKeys.AAC_INTERNAL_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_CREDENTIALS + SystemKeys.ID_SEPARATOR
            + SystemKeys.AUTHORITY_PASSWORD;

    private String credentialsId;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String verifyPassword;

    private String curPassword;

    public InternalEditableUserPassword() {
        super(SystemKeys.AUTHORITY_PASSWORD, null, null);
    }

    public InternalEditableUserPassword(String provider, String uuid) {
        super(SystemKeys.AUTHORITY_PASSWORD, provider, uuid);
    }

    public InternalEditableUserPassword(String provider, String realm, String userId, String uuid) {
        super(SystemKeys.AUTHORITY_PASSWORD, provider, uuid);
        setRealm(realm);
        setUserId(userId);
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

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
