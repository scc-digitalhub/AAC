package it.smartcommunitylab.aac.webauthn.model;

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
public class WebAuthnEditableUserCredential extends AbstractEditableUserCredentials {
    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_CREDENTIALS + SystemKeys.ID_SEPARATOR
            + SystemKeys.AUTHORITY_WEBAUTHN;

    private String credentialsId;

    @NotBlank
    private String username;

    private String userHandle;

    @NotBlank
    private String displayName;

    public WebAuthnEditableUserCredential() {
        super(SystemKeys.AUTHORITY_WEBAUTHN, null, null);
    }

    public WebAuthnEditableUserCredential(String provider, String uuid) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, provider, uuid);
    }

    public WebAuthnEditableUserCredential(String provider, String realm, String userId, String uuid) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, provider, uuid);
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

    public String getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

}
