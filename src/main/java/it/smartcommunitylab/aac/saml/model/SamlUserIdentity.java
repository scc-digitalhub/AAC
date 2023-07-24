package it.smartcommunitylab.aac.saml.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentity;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.util.Assert;

public class SamlUserIdentity extends AbstractIdentity {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_IDENTITY + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_SAML;

    // authentication principal (if available)
    private SamlUserAuthenticatedPrincipal principal;

    // user account
    private final SamlUserAccount account;

    // attribute sets
    protected Set<UserAttributes> attributes;

    public SamlUserIdentity(String authority, String provider, String realm, SamlUserAccount account) {
        super(authority, provider);
        Assert.notNull(account, "account can not be null");

        this.account = account;
        this.principal = null;
        this.attributes = Collections.emptySet();

        setUserId(account.getUserId());
        setUuid(account.getUuid());
        setRealm(realm);
    }

    public SamlUserIdentity(
        String authority,
        String provider,
        String realm,
        SamlUserAccount account,
        SamlUserAuthenticatedPrincipal principal
    ) {
        super(authority, provider);
        Assert.notNull(account, "account can not be null");

        this.account = account;
        this.principal = principal;
        this.attributes = Collections.emptySet();

        setUserId(account.getUserId());
        setUuid(account.getUuid());
        setRealm(realm);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
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
            this.attributes.addAll(attributes);
        }
    }
}
