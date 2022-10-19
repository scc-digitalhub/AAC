package it.smartcommunitylab.aac.core.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @Type(value = WebAuthnUserCredential.class, name = "credentials_webauthn"),
        @Type(value = InternalUserPassword.class, name = "credentials_password")
})
public abstract class AbstractUserCredentials extends AbstractBaseUserResource implements UserCredentials {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected AbstractUserCredentials(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, userId);
    }

    public abstract String getStatus();

    public abstract void setStatus(String status);

    public abstract void setRealm(String realm);

    public abstract void setAccountId(String accountId);
}
