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

package it.smartcommunitylab.aac.identity.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.users.model.UserResource;
import java.util.Collection;

/*
 *  An identity, bounded to a realm, is:
 *  - managed by an authority
 *  - built by a provider
 *  and contains
 *  - an account (from a provider)
 *  - a set of attributes (from a provider)
 *  - an authenticated principal when user performed auth via this provider
 *
 *  core implementations will always match account and attributes providers
 *  i.e. attributes will be fetched from identity provider
 *
 *  do note that identities *may* contain credentials in accounts OR principal.
 */
public interface UserIdentity extends UserResource {
    // authenticated principal (if available)
    public UserAuthenticatedPrincipal getPrincipal();

    // the account
    public UserAccount getAccount();

    // attributes are mapped into multiple sets
    public Collection<UserAttributes> getAttributes();

    // id is global
    // by default user identity id is the account id
    // the same id should be assigned to authenticatedPrincipal
    default String getId() {
        return getAccount() == null ? null : getAccount().getId();
    }

    default String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }
}
