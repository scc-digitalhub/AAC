package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import org.springframework.security.web.AuthenticationEntryPoint;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
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
     * convert identities from authenticatedPrincipal. for usage during login
     * 
     * if given a subjectId the idp should update the account
     */

    public UserIdentity convertIdentity(UserAuthenticatedPrincipal principal, String subjectId)
            throws NoSuchUserException;

    /*
     * fetch identities from this provider
     * 
     * implementations are not required to support this
     */

    // userId is provider-specific
    public UserIdentity getIdentity(String userId) throws NoSuchUserException;

    public UserIdentity getIdentity(String userId, boolean fetchAttributes) throws NoSuchUserException;

    /*
     * fetch for subject
     * 
     * opt-in, loads identities outside login for persisted accounts linked to the
     * subject
     * 
     * providers implementing this will enable the managers to fetch identities
     * outside the login flow!
     */

    public Collection<UserIdentity> listIdentities(String subject);

    /*
     * Login
     * 
     * at least one between url and entryPoint is required to dispatch requests. Url
     * is required to be presented in login forms, while authEntrypoint can handle
     * different kind of requests.
     */

    public String getAuthenticationUrl();

    public AuthenticationEntryPoint getAuthenticationEntryPoint();
}
