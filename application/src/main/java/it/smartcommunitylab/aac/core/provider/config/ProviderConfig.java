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

package it.smartcommunitylab.aac.core.provider.config;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import java.util.Map;

/*
 * A provider runtime configuration, built around a configMap
 */
public interface ProviderConfig<S extends ConfigMap, M extends ConfigMap> {
    /*
     * provider details
     */
    public String getRealm();

    public String getAuthority();

    public String getProvider();

    /*
     * base config
     */

    public String getName();

    public Map<String, String> getTitleMap();

    public Map<String, String> getDescriptionMap();

    /*
     * config
     */

    public M getConfigMap();

    public S getSettingsMap();

    public int getVersion();
}
