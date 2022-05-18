package it.smartcommunitylab.aac.core.base;

import java.util.Collection;
import java.util.Collections;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;

/*
 * Default Identity is an instantiable bean which contains account and identity
 */

public class DefaultIdentityImpl extends AbstractIdentity {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected final UserAuthenticatedPrincipal principal;
    protected UserAccount account;
    protected Collection<UserAttributes> attributes;

    public DefaultIdentityImpl(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, userId);
        principal = null;
        attributes = Collections.emptyList();
    }

    public DefaultIdentityImpl(String authority, String provider, String realm, UserAuthenticatedPrincipal principal) {
        super(authority, provider, realm, principal.getUserId());
        this.principal = principal;
        attributes = Collections.emptyList();
    }

    @Override
    public UserAuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public UserAccount getAccount() {
        return account;
    }

    public void setAccount(UserAccount account) {
        this.account = account;
    }

    public Collection<UserAttributes> getAttributes() {
        return attributes;
    }

    public void setAttributes(Collection<UserAttributes> attributes) {
        if (attributes != null) {
            this.attributes = Collections.unmodifiableCollection(attributes);
        }
    }

    @Override
    public String toString() {
        return "DefaultIdentityImpl [account=" + account + ", attributes=" + attributes + "]";
    }

}
