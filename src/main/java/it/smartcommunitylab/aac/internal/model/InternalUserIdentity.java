package it.smartcommunitylab.aac.internal.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentity;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalUserIdentity extends AbstractIdentity {

    // use a global version as serial uid
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    // authentication principal (if available)
    private final InternalUserAuthenticatedPrincipal principal;

    // internal user account
    private final InternalUserAccount account;

    // credentials (when available)
    // TODO evaluate exposing on identity model for all providers
    private List<? extends UserCredentials> credentials;

    // attributes map for sets associated with this identity
    private Map<String, UserAttributes> attributes;

    protected InternalUserIdentity() {
        this(SystemKeys.AUTHORITY_INTERNAL);
    }

    protected InternalUserIdentity(String authority) {
        super(authority, null, null);
        this.principal = null;
        this.account = null;
    }

    @Deprecated
    public InternalUserIdentity(String provider, String realm, InternalUserAccount account) {
        this(SystemKeys.AUTHORITY_INTERNAL, provider, realm, account);
    }

    public InternalUserIdentity(String authority, String provider, String realm, InternalUserAccount account) {
        super(authority, provider, realm);
        this.account = account;
        this.principal = null;
        this.attributes = Collections.emptyMap();
        super.setUserId(account.getUserId());
    }

    @Deprecated
    public InternalUserIdentity(String provider, String realm, InternalUserAccount account,
            InternalUserAuthenticatedPrincipal principal) {
        this(SystemKeys.AUTHORITY_INTERNAL, provider, realm, account, principal);
    }

    public InternalUserIdentity(String authority, String provider, String realm, InternalUserAccount account,
            InternalUserAuthenticatedPrincipal principal) {
        super(authority, provider, realm);
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

    public List<? extends UserCredentials> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<? extends UserCredentials> credentials) {
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
