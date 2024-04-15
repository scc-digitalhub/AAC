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

package it.smartcommunitylab.aac.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

public interface ResourceContext<R extends Resource> {
    Map<String, List<? extends R>> getResources();

    default boolean hasResources(String type) {
        Assert.notNull(type, "type cannot be null");
        return getResources().containsKey(type);
    }

    @SuppressWarnings("unchecked")
    default <T extends R> List<T> getResources(String type) {
        Assert.notNull(type, "type cannot be null");
        try {
            return !hasResources(type) ? Collections.emptyList() : (List<T>) getResources().get(type);
        } catch (ClassCastException e) {
            return Collections.emptyList();
        }
    }

    default void setResources(String type, List<? extends R> resources) {
        Assert.notNull(type, "type cannot be null");
        if (resources == null && hasResources(type)) {
            getResources().remove(type);
        } else {
            getResources().put(type, resources);
        }
    }
}
