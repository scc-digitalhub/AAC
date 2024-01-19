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

import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

public interface Context<T> {
    Map<String, List<T>> getContext();

    default boolean has(String key) {
        Assert.notNull(key, "key cannot be null");
        return getContext().containsKey(key);
    }

    @SuppressWarnings("unchecked")
    default <T> T get(String key) {
        return !has(key) ? null : (T) getContext().get(key);
    }
}