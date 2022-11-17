package it.smartcommunitylab.aac.core.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.EditableUserAccount;
import it.smartcommunitylab.aac.internal.model.InternalEditableUserAccount;

/*
 * Abstract class for editable user accounts
 * 
 * all implementations should derive from this
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "authority", visible = false)
@JsonSubTypes({
        @Type(value = InternalEditableUserAccount.class, name = "internal")
})
public abstract class AbstractEditableAccount extends AbstractBaseUserResource implements EditableUserAccount {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected String uuid;

    protected AbstractEditableAccount(String authority, String provider, String realm, String uuid) {
        super(authority, provider, realm);
        this.uuid = uuid;
    }

    protected AbstractEditableAccount(String authority, String provider, String realm, String userId, String uuid) {
        super(authority, provider, realm, userId);
        this.uuid = uuid;
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    public String getId() {
        return getUuid();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

}
