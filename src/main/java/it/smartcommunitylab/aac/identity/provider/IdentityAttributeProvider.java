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

package it.smartcommunitylab.aac.identity.provider;

import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;
import it.smartcommunitylab.aac.identity.model.UserAuthenticatedPrincipal;
import java.util.Collection;

public interface IdentityAttributeProvider<P extends UserAuthenticatedPrincipal, U extends UserAccount>
    extends ResourceProvider<UserAttributes> {
    /*
     * Fetch user attributes
     *
     * Multiple attribute sets bound to a given principal/account, authoritatively
     * provided
     */

    Collection<UserAttributes> convertPrincipalAttributes(P principal, U account);

    Collection<UserAttributes> getAccountAttributes(U account);
}
