package it.smartcommunitylab.aac.saml.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentity;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;

public class SamlUserIdentity extends AbstractIdentity {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    // authentication principal (if available)
    private SamlUserAuthenticatedPrincipal principal;

    // user account
    private final SamlUserAccount account;

    // attribute sets
    protected Set<UserAttributes> attributes;

    public SamlUserIdentity(String provider, String realm, SamlUserAccount account) {
        super(SystemKeys.AUTHORITY_SAML, provider, realm);
        this.account = account;
        this.principal = null;
        this.attributes = Collections.emptySet();
        super.setUserId(account.getUserId());
    }

    public SamlUserIdentity(String provider, String realm, SamlUserAccount account,
            SamlUserAuthenticatedPrincipal principal) {
        super(SystemKeys.AUTHORITY_SAML, provider, realm);
        this.account = account;
        this.principal = principal;
        this.attributes = Collections.emptySet();
        super.setUserId(account.getUserId());
    }

    @Override
    public SamlUserAuthenticatedPrincipal getPrincipal() {
        return this.principal;
    }

    @Override
    public SamlUserAccount getAccount() {
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
