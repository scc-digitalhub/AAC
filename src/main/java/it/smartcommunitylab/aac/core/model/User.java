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

package it.smartcommunitylab.aac.core.model;

import it.smartcommunitylab.aac.SystemKeys;
import java.util.Collection;
import java.util.Date;

/*
 * A User
 */
public interface User extends UserResource {
    //core attributes
    public String getUsername();

    public String getEmailAddress();

    public boolean isEmailVerified();

    //user registration status
    public String getStatus();

    //audit
    public Date getCreateDate();

    public Date getModifiedDate();

    //identities
    public Collection<UserIdentity> getIdentities();

    //TODO evaluate adding EditableAccounts and EditableCredentials

    default String getType() {
        return SystemKeys.RESOURCE_USER;
    }
}
