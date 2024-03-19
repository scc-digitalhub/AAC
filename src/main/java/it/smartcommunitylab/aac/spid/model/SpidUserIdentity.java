package it.smartcommunitylab.aac.spid.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.identity.base.AbstractUserIdentity;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// TODO: review whether this class is necessary, as current implementation are OIDCUserIndentity, InternalUserIdentity and
public class SpidUserIdentity extends AbstractUserIdentity {
    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_IDENTITY+SystemKeys.ID_SEPARATOR+SystemKeys.AUTHORITY_SAML;
    private SpidUserAuthenticatedPrincipal principal;

    private final SpidUserAccount account;
    private Set<UserAttributes> attributes;

    public SpidUserIdentity(
            String authority,
            String provider,
            String realm,
            SpidUserAccount account,
            SpidUserAuthenticatedPrincipal principal
    ) {
        super(authority, provider, realm, account.getUuid(), account.getUserId());
        Assert.notNull(account, "account can not be null");

        this.account = account;
        this.principal = principal;
        this.attributes = Collections.emptySet();
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public SpidUserAuthenticatedPrincipal getPrincipal() {
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
            this.attributes.addAll(attributes);
        }
    }
}
