package it.smartcommunitylab.aac.internal.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.CredentialsContainer;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentity;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

public class InternalUserIdentity extends AbstractIdentity {

    // use a global version as serial uid
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    // authentication principal (if available)
    private final InternalUserAuthenticatedPrincipal principal;

    // internal user account
    private final InternalUserAccount account;

    // credentials (when available)
    // TODO evaluate exposing on identity model for all providers
    private List<InternalUserCredential> credentials;

    // attributes map for sets associated with this identity
    private Map<String, UserAttributes> attributes;

    public InternalUserIdentity(String provider, String realm, InternalUserAccount account) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm);
        this.account = account;
        this.principal = null;
        this.attributes = Collections.emptyMap();
        super.setUserId(account.getUserId());
    }

    public InternalUserIdentity(String provider, String realm, InternalUserAccount account,
            InternalUserAuthenticatedPrincipal principal) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm);
        this.account = account;
        this.principal = principal;
        this.attributes = Collections.emptyMap();
        super.setUserId(account.getUserId());
    }

    @Override
    public InternalUserAuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public InternalUserAccount getAccount() {
        return account;
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_INTERNAL;
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

    public String getEmailAddress() {
        return account.getEmail();
    }

    public List<InternalUserCredential> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<InternalUserCredential> credentials) {
        this.credentials = credentials;
    }

//    @Override
//    public void eraseCredentials() {
//        if (this.account != null) {
//            this.account.eraseCredentials();
//        }
//        if (this.principal != null) {
//            this.principal.eraseCredentials();
//        }
//    }
//
//    public Object getCredentials() {
//        return this.account != null ? this.account.getPassword() : null;
//    }

}
