package it.smartcommunitylab.aac.core.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import it.smartcommunitylab.aac.core.model.EditableUserAccount;
import it.smartcommunitylab.aac.internal.model.InternalEditableUserAccount;

/*
 * Abstract class for editable user accounts
 * 
 * all implementations should derive from this
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @Type(value = InternalEditableUserAccount.class, name = "internal")
})
public abstract class AbstractEditableAccount extends AbstractBaseUserResource implements EditableUserAccount {

    protected String uuid;
    protected String userId;
    protected String realm;

    protected AbstractEditableAccount(String authority, String provider, String uuid) {
        super(authority, provider);
        this.uuid = uuid;
    }

    @Override
    public String getId() {
        // use uuid from editable model
        return getUuid();
    }

    @Override
    public String getResourceId() {
        return getAccountId();
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
