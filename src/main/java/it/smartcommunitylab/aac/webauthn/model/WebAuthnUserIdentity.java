package it.smartcommunitylab.aac.webauthn.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.CredentialsContainer;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.BaseIdentity;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;

public class WebAuthnUserIdentity extends BaseIdentity implements CredentialsContainer {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final WebAuthnUserAuthenticatedPrincipal principal;

    private WebAuthnUserAccount account;

    private String userId;

    private Map<String, UserAttributes> attributes;

    public WebAuthnUserIdentity(String provider,
            String realm,
            WebAuthnUserAccount account) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, provider, realm);
        this.account = account;
        this.principal = null;
        this.userId = account.getUserId();
        this.attributes = new HashMap<>();
    }

    public WebAuthnUserIdentity(String provider,
            String realm,
            WebAuthnUserAccount account,
            WebAuthnUserAuthenticatedPrincipal principal) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, provider, realm);
        this.account = account;
        this.principal = principal;
        this.userId = account.getUserId();
        this.attributes = new HashMap<>();
    }

    @Override
    public UserAuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public UserAccount getAccount() {
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

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return account.getUsername();
    }

    /*
     * userid can be resetted to properly map identity
     */

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public void eraseCredentials() {
    }

    @Override
    public String toString() {
        return "WebAuthnUserIdentity [account=" + account + ", userId=" + userId + "]";
    }
}
