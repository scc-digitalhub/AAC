package it.smartcommunitylab.aac.core.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.apple.model.AppleUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.password.model.InternalPasswordUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.model.SamlUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserAuthenticatedPrincipal;

/*
 * Abstract class for user authenticated principal
 * 
 * all implementations should derive from this
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "authority", visible = false)
@JsonSubTypes({
        @Type(value = InternalUserAuthenticatedPrincipal.class, name = SystemKeys.AUTHORITY_INTERNAL),
        @Type(value = InternalPasswordUserAuthenticatedPrincipal.class, name = SystemKeys.AUTHORITY_PASSWORD),
        @Type(value = WebAuthnUserAuthenticatedPrincipal.class, name = SystemKeys.AUTHORITY_WEBAUTHN),
        @Type(value = OIDCUserAuthenticatedPrincipal.class, name = SystemKeys.AUTHORITY_OIDC),
        @Type(value = AppleUserAuthenticatedPrincipal.class, name = SystemKeys.AUTHORITY_APPLE),
        @Type(value = SamlUserAuthenticatedPrincipal.class, name = SystemKeys.AUTHORITY_SAML)

})
public abstract class AbstractAuthenticatedPrincipal extends AbstractBaseUserResource
        implements UserAuthenticatedPrincipal {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected String username;

    protected String uuid;
    protected String emailAddress;

    protected AbstractAuthenticatedPrincipal(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    protected AbstractAuthenticatedPrincipal(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, userId);
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_PRINCIPAL;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getId() {
        return getUuid();
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getEmailAddress() {
        return emailAddress;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

}
