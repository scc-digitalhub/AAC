/**
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.model;

import java.util.Map;
import org.springframework.util.Assert;

/**
 * Map accessor with types
 */
public interface Accessor<T> {
    Map<String, T> fields();

    default boolean has(String key) {
        Assert.notNull(key, "field cannot be null");
        return fields() != null && fields().containsKey(key);
    }

    @SuppressWarnings("unchecked")
    default <K extends T> K get(String key) {
        Assert.notNull(key, "field cannot be null");
        try {
            return !has(key) ? null : (K) fields().get(key);
        } catch (ClassCastException e) {
            return null;
        }
    }
}
