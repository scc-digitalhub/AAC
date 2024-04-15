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

package it.smartcommunitylab.aac.model;

import java.io.Serializable;
import java.util.Map;

/*
 * A bean for configurable properties, which can convert to/from map
 *
 * Should be used to carry implementation specific properties over generic interfaces, replacing all the Map<> in base/default models
 */
public interface ConfigurableProperties {
    public Map<String, Serializable> getConfiguration();

    public void setConfiguration(Map<String, Serializable> props);
}
