package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.IdentityAttributeProvider;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractIdentityAttributeProvider<P extends UserAuthenticatedPrincipal, U extends UserAccount>
    extends AbstractProvider<UserAttributes>
    implements IdentityAttributeProvider<P, U> {

    protected AbstractIdentityAttributeProvider(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

    @Override
    public Collection<UserAttributes> convertPrincipalAttributes(P principal, U account) {
        String id = principal.getPrincipalId();
        Map<String, Serializable> attributes = principal.getAttributes();

        if (account instanceof AbstractAccount) {
            // set principal attrs in account
            ((AbstractAccount) account).setAttributes(attributes);
        }

        // call extract to transform
        return extractUserAttributes(account, attributes);
    }

    @Override
    public Collection<UserAttributes> getAccountAttributes(U account) {
        // account can't be null
        if (account == null) {
            throw new IllegalArgumentException();
        }

        String id = account.getAccountId();
        Map<String, Serializable> attributes = Collections.emptyMap();

        if (account instanceof AbstractAccount) {
            // read principal attrs from account
            attributes = ((AbstractAccount) account).getAttributes();
        }

        // call extract to transform
        return extractUserAttributes(account, attributes);
    }

    /*
     * Extract operation to be implemented by subclasses
     */
    protected abstract List<UserAttributes> extractUserAttributes(
        U account,
        Map<String, Serializable> principalAttributes
    );
}
