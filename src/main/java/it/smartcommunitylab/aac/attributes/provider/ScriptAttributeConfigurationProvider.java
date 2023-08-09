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

package it.smartcommunitylab.aac.attributes.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.base.AbstractAttributeConfigurationProvider;
import it.smartcommunitylab.aac.attributes.model.ConfigurableAttributeProvider;
import org.springframework.stereotype.Service;

@Service
public class ScriptAttributeConfigurationProvider
    extends AbstractAttributeConfigurationProvider<ScriptAttributeProviderConfigMap, ScriptAttributeProviderConfig> {

    public ScriptAttributeConfigurationProvider() {
        super(SystemKeys.AUTHORITY_SCRIPT);
        setDefaultConfigMap(new ScriptAttributeProviderConfigMap());
    }

    @Override
    protected ScriptAttributeProviderConfig buildConfig(ConfigurableAttributeProvider cp) {
        return new ScriptAttributeProviderConfig(cp, getConfigMap(cp.getConfiguration()));
    }
}
