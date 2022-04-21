package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;

public interface IdentityAttributeProvider<P extends UserAuthenticatedPrincipal, U extends UserAccount>
        extends ResourceProvider {

    /*
     * Fetch user attributes
     * 
     * Multiple attribute sets bound to a given principal/account, authoritatively
     * provided
     */

    Collection<UserAttributes> convertPrincipalAttributes(P principal, U account);

    Collection<UserAttributes> getAccountAttributes(U account);

    void deleteAccountAttributes(String id);

}
