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
import java.util.Collection;
import java.util.Map;

/*
 * A template handles localizable and customizable content for the UI
 */
public interface Template extends Resource {
    public String getTemplate();

    public String getLanguage();

    public Collection<String> keys();

    public String get(String key);

    public Map<String, String> getContent();

    default String getType() {
        return SystemKeys.RESOURCE_TEMPLATE;
    }

    public Map<String, Object> getModelAttributes();
}
