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

package it.smartcommunitylab.aac.attributes.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.identity.model.UserAuthenticatedPrincipal;
import java.util.Collection;

/*
 * An attribute provider offers:
 *  - custom attributes for users
 *  - attributes translation
 *  - optional persistence/synchronization
 *
 *  we don't make assumption on the internal implementation,
 *  but we expect consistent responses
 *
 *  note that additional providers (ie outside an idp) should implement only a reduced set of features:
 *  - listCustomAttributeSets
 *  - listCustomAttributes
 *  - getUserAttributes
 *  - getUserAttributes
 *  - canProvide
 *  - provideAttributes
 *
 *  they should not handle identities
 */
public interface AttributeProvider<U extends UserAttributes, M extends ConfigMap, C extends AttributeProviderConfig<M>>
    extends ConfigurableResourceProvider<U, C, AttributeProviderSettingsMap, M> {
    /*
     * Attribute sets (for scopes)
     *
     * An attribute set Provider specific, each set should be consistent for mapping
     * we don't expect provider to expose globally defined sets, those should be
     * translated.
     *
     * Implementations need to expose only *specific* attribute sets, and declare
     * which global set they support
     */
    //    public Collection<AttributeSet> listCustomAttributeSets();
    //
    //    public Collection<String> listCustomAttributes(String setId);

    //    /*
    //     * these are the global sets this provider supports.
    //     *
    //     * do note only *system* providers should support global sets, user defined
    //     * should only return their namespaced attributes.
    //     */
    //    public boolean canProvide(String globalSetId);
    //
    //    public UserAttributes provideAttributes(UserIdentity identity, String globalSetId);

    /*
     * User attributes
     *
     * Multiple attribute sets bound to a given user, authoritatively provided
     */

    /*
     * The provider should expose translation between their representation and the
     * global schema. Implementations are supposed to check if they can handle the
     * model (for example if class matches their model)
     *
     * Could be a no-op, could expose only a subset..
     *
     * we usually expect at least sets for: profile, email
     *
     * note that we expect all sets declared as supported present in responses,
     * filled as good as possible given the identity provided.
     *
     * Sets returned from identity conversion are *always* used for token claims
     */

    public Collection<UserAttributes> convertPrincipalAttributes(UserAuthenticatedPrincipal principal, String userId);

    //    public Collection<UserAttributes> convertAttributes(Map<String, Serializable> attributes);

    //    public UserAttributes convertAttributes(UserAttributes attributes);

    /*
     * Fetch attributes for users
     *
     * note that we don't expect providers to be able to dynamically serve
     * attributes, if those are not persisted/cached this methods could return empty
     * attributes.
     *
     * we will call these to fetch api responses outside token handling
     */
    //
    //    public UserAttributes getAttributes(String subject) throws NoSuchUserException;

    // we expect the list to contain both custom and global sets, according to
    // supported, if available
    public Collection<UserAttributes> getUserAttributes(String userId);

    //    public Collection<UserAttributes> getAccountAttributes(String id);

    public UserAttributes getUserAttributes(String userId, String setId) throws NoSuchAttributeSetException;

    public void deleteUserAttributes(String userId);

    public void deleteUserAttributes(String userId, String setId) throws NoSuchAttributeSetException;

    //    public void deleteAccountAttributes(String id);

    public default String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }
}
