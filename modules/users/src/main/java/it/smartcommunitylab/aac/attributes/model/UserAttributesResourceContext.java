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

package it.smartcommunitylab.aac.attributes.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.ResourceContext;
import it.smartcommunitylab.aac.model.UserResource;
import it.smartcommunitylab.aac.model.UserResourceContext;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface UserAttributesResourceContext extends UserResourceContext {
    default Collection<UserAttributes> getAttributes() {
        return getResources(SystemKeys.RESOURCE_ATTRIBUTES);
    }

    default void setAttributes(List<UserAttributes> attributes) {
        setResources(SystemKeys.RESOURCE_ATTRIBUTES, attributes);
    }

    static UserAttributesResourceContext with(ResourceContext<UserResource> context) {
        return () -> context.getResources();
    }

    static UserAttributesResourceContext from(ResourceContext<UserResource> context) {
        return () -> Collections.unmodifiableMap(context.getResources());
    }

    @JsonIgnore
    Map<String, List<UserResource>> getResources();
}
