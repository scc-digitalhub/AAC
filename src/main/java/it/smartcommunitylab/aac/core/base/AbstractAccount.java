package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;

/*
 * Abstract class for user accounts
 * 
 * all implementations should derive from this
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @Type(value = InternalUserAccount.class, name = InternalUserAccount.RESOURCE_TYPE),
        @Type(value = OIDCUserAccount.class, name = OIDCUserAccount.RESOURCE_TYPE),
        @Type(value = SamlUserAccount.class, name = SamlUserAccount.RESOURCE_TYPE)
})
public abstract class AbstractAccount extends AbstractBaseUserResource implements UserAccount {

    protected AbstractAccount(String authority, String provider) {
        super(authority, provider);
    }

    @Override
    public String getId() {
        // use uuid from persisted model
        return getUuid();
    }

    // uuid is mandatory
    public abstract String getUuid();

    // account status is manageable
    public abstract String getStatus();

    public abstract void setStatus(String status);

    // accounts store attributes as maps
    public abstract Map<String, Serializable> getAttributes();

    public abstract void setAttributes(Map<String, Serializable> attributes);

}
