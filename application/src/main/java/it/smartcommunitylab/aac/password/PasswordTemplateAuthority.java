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

package it.smartcommunitylab.aac.password;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.authorities.AbstractSingleConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.password.provider.PasswordTemplateProvider;
import it.smartcommunitylab.aac.templates.TemplateProviderAuthority;
import it.smartcommunitylab.aac.templates.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfig;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfigurationProvider;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderConfigMap;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderSettingsMap;
import it.smartcommunitylab.aac.templates.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class PasswordTemplateAuthority
    extends AbstractSingleConfigurableProviderAuthority<PasswordTemplateProvider, ConfigurableTemplateProvider, RealmTemplateProviderConfig, TemplateProviderSettingsMap, TemplateProviderConfigMap>
    implements
        TemplateProviderAuthority<PasswordTemplateProvider, TemplateModel, RealmTemplateProviderConfig, TemplateProviderConfigMap> {

    // services
    private final TemplateService templateService;

    // configuration provider
    protected RealmTemplateProviderConfigurationProvider configProvider;

    public PasswordTemplateAuthority(
        TemplateService templateService,
        ProviderConfigRepository<RealmTemplateProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_PASSWORD, new PasswordConfigTranslatorRepository(registrationRepository));
        Assert.notNull(templateService, "template service is mandatory");

        this.templateService = templateService;
    }

    // @Override
    // public String getType() {
    //     return SystemKeys.RESOURCE_TEMPLATE;
    // }

    @Autowired
    public void setConfigProvider(RealmTemplateProviderConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Override
    public RealmTemplateProviderConfigurationProvider getConfigurationProvider() {
        return configProvider;
    }

    @Override
    protected PasswordTemplateProvider buildProvider(RealmTemplateProviderConfig config) {
        PasswordTemplateProvider p = new PasswordTemplateProvider(
            config.getProvider(),
            templateService,
            config,
            config.getRealm()
        );

        return p;
    }

    static class PasswordConfigTranslatorRepository
        extends TranslatorProviderConfigRepository<RealmTemplateProviderConfig, RealmTemplateProviderConfig> {

        public PasswordConfigTranslatorRepository(
            ProviderConfigRepository<RealmTemplateProviderConfig> externalRepository
        ) {
            super(externalRepository);
            setConverter(config -> {
                RealmTemplateProviderConfig c = new RealmTemplateProviderConfig(
                    SystemKeys.AUTHORITY_PASSWORD,
                    config.getProvider(),
                    config.getRealm()
                );

                c.setSettingsMap(config.getSettingsMap());
                c.setConfigMap(config.getConfigMap());
                c.setVersion(config.getVersion());

                return c;
            });
        }
    }
}
