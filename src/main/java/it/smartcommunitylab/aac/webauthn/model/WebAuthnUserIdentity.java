package it.smartcommunitylab.aac.webauthn.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.CredentialsContainer;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentity;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;

public class WebAuthnUserIdentity extends AbstractIdentity implements CredentialsContainer {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    // authentication principal (if available)
    private final WebAuthnUserAuthenticatedPrincipal principal;

    // user account
    private final WebAuthnUserAccount account;

    // attributes map for sets associated with this identity
    private Map<String, UserAttributes> attributes;

    public WebAuthnUserIdentity(String provider,
            String realm,
            WebAuthnUserAccount account) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, provider, realm);
        this.account = account;
        this.principal = null;
        this.attributes = Collections.emptyMap();
        super.setUserId(account.getUserId());
    }

    public WebAuthnUserIdentity(String provider,
            String realm,
            WebAuthnUserAccount account,
            WebAuthnUserAuthenticatedPrincipal principal) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, provider, realm);
        this.account = account;
        this.principal = principal;
        this.attributes = Collections.emptyMap();
        super.setUserId(account.getUserId());
    }

    @Override
    public WebAuthnUserAuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public WebAuthnUserAccount getAccount() {
        return account;
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_WEBAUTHN;
    }

    @Override
    public Collection<UserAttributes> getAttributes() {
        return attributes.values();
    }

    public void setAttributes(Collection<UserAttributes> attributes) {
        this.attributes = new HashMap<>();
        if (attributes != null) {
            attributes.forEach(a -> this.attributes.put(a.getIdentifier(), a));
        }
    }

    public String getUsername() {
        return account.getUsername();
    }

    public String getUserHandle() {
        return account.getUserHandle();
    }

    public String getEmailAddress() {
        return account.getEmailAddress();
    }

    @Override
    public void eraseCredentials() {
        if (this.account != null) {
            this.account.eraseCredentials();
        }
        if (this.principal != null) {
            this.principal.eraseCredentials();
        }
    }

    public Object getCredentials() {
        return null;
    }
}
