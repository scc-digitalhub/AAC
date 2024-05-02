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

package it.smartcommunitylab.aac.attributes.base;

import it.smartcommunitylab.aac.attributes.provider.AttributeConfigurationProvider;
import it.smartcommunitylab.aac.attributes.provider.AttributeProviderAuthority;
import it.smartcommunitylab.aac.attributes.provider.AttributeProviderSettingsMap;
import it.smartcommunitylab.aac.attributes.provider.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.base.authorities.AbstractConfigurableProviderAuthority;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

public abstract class AbstractAttributeAuthority<
    S extends AbstractAttributeProvider<? extends AbstractUserAttributes, C, M>,
    M extends AbstractConfigMap,
    C extends AbstractAttributeProviderConfig<M>
>
    extends AbstractConfigurableProviderAuthority<S, ConfigurableAttributeProvider, C, AttributeProviderSettingsMap, M>
    implements AttributeProviderAuthority<S, C, M> {

    // attributes sets service
    protected final AttributeService attributeService;

    // configuration provider
    protected AttributeConfigurationProvider<C, M> configProvider;

    protected AbstractAttributeAuthority(
        String authorityId,
        AttributeService attributeService,
        ProviderConfigRepository<C> registrationRepository
    ) {
        super(authorityId, registrationRepository);
        Assert.notNull(attributeService, "attribute service is mandatory");

        this.attributeService = attributeService;
    }

    // @Override
    // public String getType() {
    //     return SystemKeys.RESOURCE_ATTRIBUTES;
    // }

    @Override
    public AttributeConfigurationProvider<C, M> getConfigurationProvider() {
        return configProvider;
    }

    @Autowired
    public void setConfigProvider(AttributeConfigurationProvider<C, M> configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(configProvider, "config provider is mandatory");
    }
}
