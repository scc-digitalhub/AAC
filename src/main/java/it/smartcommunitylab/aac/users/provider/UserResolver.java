/**
 * Copyright 2023 Fondazione Bruno Kessler
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

package it.smartcommunitylab.aac.users.provider;

import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;
import it.smartcommunitylab.aac.identity.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.users.model.User;

/*
 * User resolvers are providers which resolve the relation between user resources and owners.
 * When the provider has no solution, it is expected to return null
 * When needed, providers can *create* users to resolve the lookup.
 */
public interface UserResolver extends ResourceProvider<User> {
    //resolve by looking at principal attributes
    public User resolveByPrincipal(UserAuthenticatedPrincipal p);

    //resolve by looking at account attributes
    public User resolveByAccount(UserAccount a);
}
