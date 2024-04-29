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

package it.smartcommunitylab.aac.identity.provider;

import it.smartcommunitylab.aac.core.model.ProviderConfig;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.util.Map;

public interface IdentityProviderConfig<M extends ConfigMap> extends ProviderConfig<IdentityProviderSettingsMap, M> {
    public boolean isLinkable();

    //TODO map events as ENUM
    public String getEvents();

    public int getPosition();

    //TODO refactor hooks
    public Map<String, String> getHookFunctions();
}
