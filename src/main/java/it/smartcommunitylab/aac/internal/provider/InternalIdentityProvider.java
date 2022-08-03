package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.UserCredentialsService;

public interface InternalIdentityProvider<I extends UserIdentity, U extends UserAccount, P extends UserAuthenticatedPrincipal, C extends UserCredentials>
        extends IdentityProvider<I> {

    public UserCredentialsService<C> getCredentialsService();

    public String getRegistrationUrl();

//    public String getLoginForm();

//    public String getCredentialsUrl();
}
