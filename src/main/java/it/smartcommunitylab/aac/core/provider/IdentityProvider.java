package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.dto.LoginProvider;

/*
 * Identity providers handle authentication for users and produce a valid user identity
 * 
 * An identity is composed by an account, bounded to the provider, and one or more attribute sets.
 * At minimum, we expect every provider to fulfill core attribute sets (basic, email, openid, account).
 */

public interface IdentityProvider<I extends UserIdentity>
        extends ResourceProvider {

    public static final String ATTRIBUTE_MAPPING_FUNCTION = "attributeMapping";

    /*
     * Config
     */
    public String getName();

    public String getDescription();

    public AbstractIdentityProviderConfig getConfig();

    /*
     * auth provider
     */
    public ExtendedAuthenticationProvider<? extends UserAuthenticatedPrincipal, ? extends UserAccount> getAuthenticationProvider();

    /*
     * internal providers
     */
    public AccountProvider<? extends UserAccount> getAccountProvider();

    public IdentityAttributeProvider<? extends UserAuthenticatedPrincipal, ? extends UserAccount> getAttributeProvider();

    /*
     * subjects are global, we can resolve
     */

    public SubjectResolver<? extends UserAccount> getSubjectResolver();

    /*
     * convert identities from authenticatedPrincipal. Used for login only.
     * 
     * If given a subjectId the provider should update the account
     */

    public I convertIdentity(UserAuthenticatedPrincipal principal, String userId)
            throws NoSuchUserException;

    /*
     * fetch identities from this provider
     * 
     * implementations are not required to support this
     */

    // uuid is global
    public I findIdentityByUuid(String uuid);

    // identityId is provider-specific
    public I findIdentity(String identityId);

    public I getIdentity(String identityId) throws NoSuchUserException;

    public I getIdentity(String identityId, boolean fetchAttributes)
            throws NoSuchUserException;

    /*
     * fetch for user
     * 
     * opt-in, loads identities outside login for persisted accounts linked to the
     * subject
     * 
     * providers implementing this will enable the managers to fetch identities
     * outside the login flow!
     */

    public Collection<I> listIdentities(String userId);

    public Collection<I> listIdentities(String userId, boolean fetchAttributes);

    /*
     * Link account
     * 
     * Providers must expose the ability to link/relink identities to a given user
     */
    public I linkIdentity(String userId, String identityId) throws NoSuchUserException;

    /*
     * Delete accounts.
     * 
     * Implementations are required to implement this, even as a no-op. At minimum
     * we expect providers to clean up any local registration or cache.
     */
    public void deleteIdentity(String identityId) throws NoSuchUserException;

    public void deleteIdentities(String userId);

    /*
     * Login
     * 
     * Url is required to be presented in login forms, while authEntrypoint can
     * handle different kind of requests.
     */

    public String getAuthenticationUrl();

    public LoginProvider getLoginProvider();

//    public AuthenticationEntryPoint getAuthenticationEntryPoint();

//    /*
//     * Additional action urls
//     */
//    public Map<String, String> getActionUrls();
}
