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

import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.model.Subject;

public interface SubjectResolver<A extends UserAccount> extends ResourceProvider<A> {
    /*
     * Local id: direct resolve
     *
     * we expect providers to be able to resolve subjects for persisted accounts
     */

    public Subject resolveByAccountId(String accountId);

    public Subject resolveByPrincipalId(String principalId);

    public Subject resolveByIdentityId(String identityId);

    /*
     * Identifying attributes
     *
     * an account identifier (outside local id) which is valid only for the same
     * provider to identify multiple accounts as belonging to the same user.
     *
     * (ex. transient ids on responses, persistent value as attribute)
     *
     * we require idps to set identifying attribute as username
     */
    public Subject resolveByUsername(String accountId);

    /*
     * Account linking
     *
     * A set of attributes which, when matched by other resolvers, enables linking
     * to the same subject across providers.
     *
     * we require idps to provide resolution at least via email
     */
    public Subject resolveByEmailAddress(String accountId);
    /*
     * Attributes resolution
     *
     * dynamic resolution via configurable attributes
     *
     * DISABLED
     */
    //    // TODO re-evaluate account linking for 2 scenarios:
    //    // multi-login and
    //    // additional-identity-fetch
    //
    ////    public Subject resolveByLinkingAttributes(Map<String, String> attributes);
    //    public Subject resolveByAttributes(Map<String, String> attributes);
    //
    //    // disabled exposure of attribute keys
    ////    public Collection<String> getLinkingAttributes();
    //
    ////    public Map<String, String> getLinkingAttributes(UserAuthenticatedPrincipal principal);
    //    public Map<String, String> getAttributes(UserAuthenticatedPrincipal principal);

}
