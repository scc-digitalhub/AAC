package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.model.UserIdentity;

/*
 * Identity providers handle authentication for users and produce a valid user identity
 * 
 * Authentication is handled with AuthenticationTokens which carry authentication details along with
 * UserPrincipal details. 
 * An identity is composed by an account, bounded to the provider, and one or more attribute sets.
 * At minimum, we expect every provider to fulfill core attribute sets (basic, email, openid, account).
 */

public interface IdentityProvider<I extends UserIdentity, M extends ConfigMap, C extends IdentityProviderConfig<M>>
        extends ConfigurableResourceProvider<UserIdentity, ConfigurableIdentityProvider, M, C> {

    public static final String ATTRIBUTE_MAPPING_FUNCTION = "attributeMapping";

    /*
     * Config
     */
    public String getName();

    public String getDescription();

    public C getConfig();

    /*
     * Authoritative for the given identity model
     * 
     * only authoritative providers should edit accounts, expose resolvers etc,
     * while non-authoritative should handle only authentication + credentials +
     * attributes when available
     */
    public boolean isAuthoritative();

    /*
     * Authentication provider handles the processing of AuthenticationTokens and
     * the resolution of UserPrincipal. Optionally they can expose a UserAccount
     * matching the principal, if available in the provider.
     */
    public ExtendedAuthenticationProvider<? extends UserAuthenticatedPrincipal, ? extends UserAccount> getAuthenticationProvider();

    /*
     * Account provider acts as the source for user accounts, when the details are
     * persisted in the provider or available for requests. Do note that idps are
     * not required to persist accounts.
     */
    public AccountProvider<? extends UserAccount> getAccountProvider();

    /*
     * Attribute providers retrieve and format user properties available to the
     * provider as UserAttributes bounded to the UserIdentity exposed to the outside
     * world.
     */

    public IdentityAttributeProvider<? extends UserAuthenticatedPrincipal, ? extends UserAccount> getAttributeProvider();

    /*
     * Subject resolvers can discover a matching user by receiving identifying
     * properties (such as email) and looking at locally (in the provider)
     * registered accounts to find an existing identity for the same user.
     */

    public SubjectResolver<? extends UserAccount> getSubjectResolver();

    /*
     * Convert identities from authenticatedPrincipal. Used for login only.
     * 
     * If given a subjectId the provider should either update the account to link
     * the user, or reject the conversion.
     */

    public I convertIdentity(UserAuthenticatedPrincipal principal, String userId)
            throws NoSuchUserException;

    /*
     * Fetch identities from this provider
     * 
     * Do note that implementations are not required to support this.
     */

//    // uuid is global
//    public I findIdentityByUuid(String uuid);

    // identityId is provider-specific
    public I findIdentity(String userId, String identityId);

    public I getIdentity(String userId, String identityId) throws NoSuchUserException;

    public I getIdentity(String userId, String identityId, boolean fetchAttributes)
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
    public void deleteIdentity(String userId, String identityId) throws NoSuchUserException;

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
