/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import java.util.Collection;

/*
 * Identity providers handle authentication for users and produce a valid user identity
 *
 * Authentication is handled with AuthenticationTokens which carry authentication details along with
 * UserPrincipal details.
 * An identity is composed by an account, bounded to the provider, and one or more attribute sets.
 * At minimum, we expect every provider to fulfill core attribute sets (basic, email, openid, account).
 */

public interface IdentityProvider<
    I extends UserIdentity,
    U extends UserAccount,
    P extends UserAuthenticatedPrincipal,
    M extends ConfigMap,
    C extends IdentityProviderConfig<M>
>
    extends ConfigurableResourceProvider<I, ConfigurableIdentityProvider, M, C> {
    public static final String ATTRIBUTE_MAPPING_FUNCTION = "attributeMapping";

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
    public ExtendedAuthenticationProvider<P, U> getAuthenticationProvider();

    /*
     * Subject resolvers can discover a matching user by receiving identifying
     * properties (such as email) and looking at locally (in the provider)
     * registered accounts to find an existing identity for the same user.
     */

    public SubjectResolver<U> getSubjectResolver();

    /*
     * Convert identities from authenticatedPrincipal. Used for login only.
     *
     * If given a subjectId the provider should either update the account to link
     * the user, or reject the conversion.
     */

    public I convertIdentity(UserAuthenticatedPrincipal principal, String userId)
        throws NoSuchUserException, RegistrationException;

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

    public I getIdentity(String userId, String identityId, boolean fetchAttributes) throws NoSuchUserException;

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
    public I linkIdentity(String userId, String identityId) throws NoSuchUserException, RegistrationException;

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

    public default String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }
}
