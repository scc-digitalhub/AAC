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

package it.smartcommunitylab.aac.templates.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.provider.AbstractConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.templates.model.ConfigurableTemplateProvider;
import org.springframework.stereotype.Service;

@Service
public class RealmTemplateProviderConfigurationProvider
    extends AbstractConfigurationProvider<
        RealmTemplateProviderConfig,
        ConfigurableTemplateProvider,
        TemplateProviderSettingsMap,
        TemplateProviderConfigMap
    >
    implements TemplateProviderConfigurationProvider<RealmTemplateProviderConfig, TemplateProviderConfigMap> {

    public RealmTemplateProviderConfigurationProvider(
        ProviderConfigRepository<RealmTemplateProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_TEMPLATE, registrationRepository);
        setDefaultConfigMap(new TemplateProviderConfigMap());
    }

    @Override
    protected RealmTemplateProviderConfig buildConfig(ConfigurableTemplateProvider cp) {
        return new RealmTemplateProviderConfig(
            cp,
            getSettingsMap(cp.getSettings()),
            getConfigMap(cp.getConfiguration())
        );
    }

    @Override
    public ConfigurableTemplateProvider buildConfigurable(RealmTemplateProviderConfig providerConfig) {
        ConfigurableTemplateProvider cp = new ConfigurableTemplateProvider(
            providerConfig.getAuthority(),
            providerConfig.getProvider(),
            providerConfig.getRealm()
        );
        cp.setName(providerConfig.getName());
        cp.setTitleMap(providerConfig.getTitleMap());
        cp.setDescriptionMap(providerConfig.getDescriptionMap());

        cp.setSettings(getConfiguration(providerConfig.getSettingsMap()));
        cp.setConfiguration(getConfiguration(providerConfig.getConfigMap()));

        // provider config are active by definition
        cp.setEnabled(true);

        return cp;
    }
}
