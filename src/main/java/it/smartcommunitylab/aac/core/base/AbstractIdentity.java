package it.smartcommunitylab.aac.core.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.openid.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.saml.model.SamlUserIdentity;

/*
 * Abstract identity 
 * 
 * all implementations should derive from this
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "authority")
@JsonSubTypes({
        @Type(value = InternalUserIdentity.class, name = "internal"),
        @Type(value = OIDCUserIdentity.class, name = "oidc"),
        @Type(value = SamlUserIdentity.class, name = "saml")

})
public abstract class AbstractIdentity extends AbstractBaseUserResource implements UserIdentity {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected AbstractIdentity(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    protected AbstractIdentity(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, userId);
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    @Override
    public String getId() {
        // by default return the addressable resource id as identity id
        // account id is always accessible via getAccount
        return this.getResourceId();
    }

    // resource is globally unique and addressable
    // ie given to an external actor he should be able to find the authority and
    // then the provider to request this resource
    @Override
    public String getResourceId() {
        if (getAccount() == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getAuthority()).append(SystemKeys.ID_SEPARATOR);
        sb.append(getProvider()).append(SystemKeys.ID_SEPARATOR);
        sb.append(getAccount().getId());

        return sb.toString();
    }

}
