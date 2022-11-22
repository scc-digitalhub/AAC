package it.smartcommunitylab.aac.core.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.openid.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.saml.model.SamlUserIdentity;

/*
 * Abstract identity 
 * 
 * all implementations should derive from this
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @Type(value = InternalUserIdentity.class, name = InternalUserIdentity.RESOURCE_TYPE),
        @Type(value = OIDCUserIdentity.class, name = OIDCUserIdentity.RESOURCE_TYPE),
        @Type(value = SamlUserIdentity.class, name = SamlUserIdentity.RESOURCE_TYPE)
})
public abstract class AbstractIdentity extends AbstractBaseUserResource implements UserIdentity {

    private String uuid;
    private String userId;
    private String realm;

    protected AbstractIdentity(String authority, String provider) {
        super(authority, provider);
    }

    @Override
    public String getId() {
        // use uuid from persisted model
        return getUuid();
    }

    @Override
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
