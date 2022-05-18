package it.smartcommunitylab.aac.openid.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentity;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;

public class OIDCUserIdentity extends AbstractIdentity {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    // authentication principal (if available)
    private final OIDCUserAuthenticatedPrincipal principal;

    // user account
    private final OIDCUserAccount account;

    // attribute sets
    protected Set<UserAttributes> attributes;

    public OIDCUserIdentity(String provider, String realm, OIDCUserAccount account) {
        this(SystemKeys.AUTHORITY_OIDC, provider, realm, account);
    }

    public OIDCUserIdentity(String authority, String provider, String realm, OIDCUserAccount account) {
        super(authority, provider, realm);
        this.account = account;
        this.principal = null;
        this.attributes = Collections.emptySet();
        super.setUserId(account.getUserId());
    }

    public OIDCUserIdentity(String provider, String realm, OIDCUserAccount account,
            OIDCUserAuthenticatedPrincipal principal) {
        this(SystemKeys.AUTHORITY_OIDC, provider, realm, account, principal);
    }

    public OIDCUserIdentity(String authority, String provider, String realm, OIDCUserAccount account,
            OIDCUserAuthenticatedPrincipal principal) {
        super(authority, provider, realm);
        this.account = account;
        this.principal = principal;
        this.attributes = Collections.emptySet();
        super.setUserId(account.getUserId());
    }

    @Override
    public OIDCUserAuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public OIDCUserAccount getAccount() {
        return account;
    }

    @Override
    public Collection<UserAttributes> getAttributes() {
        return attributes;
    }

    public String getSubject() {
        return account.getSubject();
    }

    public String getEmailAddress() {
        return account.getEmail();
    }

    public void setAttributes(Collection<UserAttributes> attributes) {
        this.attributes = new HashSet<>();
        if (attributes != null) {
            this.attributes.addAll(attributes);
        }
    }

}