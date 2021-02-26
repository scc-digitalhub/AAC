package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.model.UserIdentity;

public interface IdentityProvider extends ResourceProvider {

    /*
     * auth provider
     */
    public ExtendedAuthenticationProvider getAuthenticationProvider();

    /*
     * internal providers
     */
    public AccountProvider getAccountProvider();

    public AttributeProvider getAttributeProvider();

    /*
     * subjects are global, we can resolve
     */

    public SubjectResolver getSubjectResolver();

    /*
     * convert identities from authenticatedPrincipal
     */

    public UserIdentity convertIdentity(UserAuthenticatedPrincipal principal) throws NoSuchUserException;

    /*
     * fetch identities from this provider
     * 
     * implementations are not required to support this
     */

    // userId is provider-specific
    public UserIdentity getIdentity(String userId) throws NoSuchUserException;

    public UserIdentity getIdentity(String userId, boolean fetchAttributes) throws NoSuchUserException;

    public Collection<UserIdentity> listIdentities(String subject);

}
