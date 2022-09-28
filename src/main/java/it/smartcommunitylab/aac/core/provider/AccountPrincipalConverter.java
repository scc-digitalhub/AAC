package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;

public interface AccountPrincipalConverter<U extends UserAccount> extends ResourceProvider<U> {
    /*
     * Build account from principal attributes
     */
    public U convertAccount(UserAuthenticatedPrincipal principal, String userId);
}
