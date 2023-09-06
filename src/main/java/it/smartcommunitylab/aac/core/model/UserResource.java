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

package it.smartcommunitylab.aac.core.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.repository.JsonSchemaIgnore;

/*
 * A realm scoped user resource, provided by an authority via a specific provider
 */

public interface UserResource extends Resource {
    public String getUserId();

    // uuid is global
    public String getUuid();

    @Override
    @JsonSchemaIgnore
    public default String getUrn() {
        StringBuilder sb = new StringBuilder();
        sb.append(SystemKeys.URN_PROTOCOL);
        sb.append(getType()).append(SystemKeys.URN_SEPARATOR);
        sb.append(getAuthority()).append(SystemKeys.ID_SEPARATOR);
        sb.append(getProvider()).append(SystemKeys.ID_SEPARATOR);
        sb.append(getResourceId());

        return sb.toString();
    }
}
