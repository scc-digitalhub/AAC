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

package it.smartcommunitylab.aac.credentials.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ResourceContext;
import it.smartcommunitylab.aac.users.model.UserResource;
import it.smartcommunitylab.aac.users.model.UserResourceContext;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface UserCredentialsResourceContext extends UserResourceContext {
    default Collection<UserCredentials> getCredentials() {
        return getResources(SystemKeys.RESOURCE_CREDENTIALS);
    }

    default void setCredentials(List<UserCredentials> credentials) {
        setResources(SystemKeys.RESOURCE_CREDENTIALS, credentials);
    }

    static UserCredentialsResourceContext with(ResourceContext<UserResource> context) {
        return () -> context.getResources();
    }

    static UserCredentialsResourceContext from(ResourceContext<UserResource> context) {
        return () -> Collections.unmodifiableMap(context.getResources());
    }
}
