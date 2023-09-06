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

package it.smartcommunitylab.aac.core.persistence;

import java.io.Serializable;
import java.util.Map;

public interface ProviderEntity {
    public String getRealm();

    public String getAuthority();

    public String getProvider();

    public String getName();

    public Map<String, String> getTitleMap();

    public Map<String, String> getDescriptionMap();

    public boolean isEnabled();

    public int getVersion();

    public void setAuthority(String authority);

    public void setProvider(String provider);

    public void setRealm(String realm);

    public void setConfigurationMap(Map<String, Serializable> configurationMap);
}
