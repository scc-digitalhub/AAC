package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;

/*
 * Abstract class for user accounts
 * 
 * all implementations should derive from this
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "authority", visible = false)
@JsonSubTypes({
        @Type(value = InternalUserAccount.class, name = "internal"),
        @Type(value = OIDCUserAccount.class, name = "oidc"),
        @Type(value = SamlUserAccount.class, name = "saml")

})
public abstract class AbstractAccount extends AbstractBaseUserResource implements UserAccount {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected AbstractAccount(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    protected AbstractAccount(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, userId);
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    public String getId() {
        return getUuid();
    }

    public abstract String getStatus();

    public abstract void setStatus(String status);

    public abstract void setUuid(String uuid);

    public abstract void setRealm(String realm);

    public abstract void setUserId(String userId);

    // by default accounts are associated to repositories, not providers
    // authorityId and provider are transient
    public abstract void setAuthority(String authority);

    public abstract void setProvider(String provider);

    // accounts store attributes as maps
    public abstract Map<String, Serializable> getAttributes();

    public abstract void setAttributes(Map<String, Serializable> attributes);

}
