package it.smartcommunitylab.aac.spid.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentity;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.spid.auth.SpidAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;

public class SpidUserIdentity extends AbstractIdentity {

    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;

    // authentication principal (if available)
    private SpidAuthenticatedPrincipal principal;

    // user account
    private final SpidUserAccount account;

    // attribute sets
    protected Set<UserAttributes> attributes;

    public SpidUserIdentity(String provider, String realm, SpidUserAccount account) {
        super(SystemKeys.AUTHORITY_SPID, provider, realm);
        this.account = account;
        this.principal = null;
        this.attributes = Collections.emptySet();
        super.setUserId(account.getUserId());
    }

    public SpidUserIdentity(String provider, String realm, SpidUserAccount account,
            SpidAuthenticatedPrincipal principal) {
        super(SystemKeys.AUTHORITY_SAML, provider, realm);
        this.account = account;
        this.principal = principal;
        this.attributes = Collections.emptySet();
        super.setUserId(account.getUserId());
    }

    @Override
    public SpidAuthenticatedPrincipal getPrincipal() {
        return this.principal;
    }

    @Override
    public SpidUserAccount getAccount() {
        return account;
    }

    @Override
    public Collection<UserAttributes> getAttributes() {
        return attributes;
    }

    public String getSubjectId() {
        return account.getSubjectId();
    }

    public String getEmailAddress() {
        return account.getEmail();
    }

    public void setAttributes(Collection<UserAttributes> attributes) {
        this.attributes = new HashSet<>();
        if (attributes != null) {
            attributes.addAll(attributes);
        }
    }

}
