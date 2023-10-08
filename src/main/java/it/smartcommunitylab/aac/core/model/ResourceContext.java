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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

public interface ResourceContext {
    Map<String, List<? extends Resource>> getResources();

    default boolean hasResources(String type) {
        Assert.notNull(type, "type cannot be null");
        return getResources().containsKey(type);
    }

    @SuppressWarnings("unchecked")
    default <T extends Resource> List<T> getResources(String type) {
        Assert.notNull(type, "type cannot be null");
        return !hasResources(type) ? Collections.emptyList() : (List<T>) getResources().get(type);
    }

    default void setResources(String type, List<? extends Resource> resources) {
        Assert.notNull(type, "type cannot be null");
        if (resources == null && hasResources(type)) {
            getResources().remove(type);
        } else {
            getResources().put(type, resources);
        }
    }
}
