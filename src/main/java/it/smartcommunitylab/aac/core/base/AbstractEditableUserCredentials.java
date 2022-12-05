package it.smartcommunitylab.aac.core.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import it.smartcommunitylab.aac.core.model.EditableUserCredentials;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.password.dto.InternalEditableUserPassword;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;

/*
 * Abstract class for editable user credentials
 * 
 * all implementations should derive from this
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
//        @Type(value = WebAuthnEditableUserCredential.class, name = WebAuthnEditableUserCredential.RESOURCE_TYPE),
        @Type(value = InternalEditableUserPassword.class, name = InternalEditableUserPassword.RESOURCE_TYPE)
})
public abstract class AbstractEditableUserCredentials extends AbstractBaseUserResource
        implements EditableUserCredentials {

    protected String uuid;
    protected String userId;
    protected String realm;

    protected AbstractEditableUserCredentials(String authority, String provider, String uuid) {
        super(authority, provider);
        this.uuid = uuid;
    }

    @Override
    public String getId() {
        // use uuid from persisted model
        return getUuid();
    }

    @Override
    public String getResourceId() {
        return getCredentialsId();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }
}
