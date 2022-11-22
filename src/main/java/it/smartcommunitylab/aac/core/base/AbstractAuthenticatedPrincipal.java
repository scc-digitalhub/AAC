package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.password.model.InternalPasswordUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.model.SamlUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserAuthenticatedPrincipal;

/*
 * Abstract class for user authenticated principal
 * 
 * all implementations should derive from this
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @Type(value = InternalUserAuthenticatedPrincipal.class, name = SystemKeys.AUTHORITY_INTERNAL),
        @Type(value = InternalPasswordUserAuthenticatedPrincipal.class, name = SystemKeys.AUTHORITY_PASSWORD),
        @Type(value = WebAuthnUserAuthenticatedPrincipal.class, name = SystemKeys.AUTHORITY_WEBAUTHN),
        @Type(value = OIDCUserAuthenticatedPrincipal.class, name = SystemKeys.AUTHORITY_OIDC),
        @Type(value = SamlUserAuthenticatedPrincipal.class, name = SystemKeys.AUTHORITY_SAML)
})
public abstract class AbstractAuthenticatedPrincipal extends AbstractBaseUserResource
        implements UserAuthenticatedPrincipal {

    private String uuid;
    private String userId;
    private String realm;

    protected AbstractAuthenticatedPrincipal(String authority, String provider) {
        super(authority, provider);
    }

    public abstract String getUsername();

    @Override
    public String getId() {
        // use uuid from model
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

    @Override
    public Map<String, Serializable> getAttributes() {
        Map<String, Serializable> map = new HashMap<>();
        map.put("authority", getAuthority());
        map.put("provider", getProvider());
        map.put("username", getUsername());
        map.put("principalId", getPrincipalId());
        map.put("id", getId());

        if (StringUtils.hasText(uuid)) {
            map.put("uuid", uuid);
        }
        if (StringUtils.hasText(userId)) {
            map.put("userId", userId);
        }
        if (StringUtils.hasText(realm)) {
            map.put("realm", realm);
        }

        return map;
    }

}
