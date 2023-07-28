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

package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.core.authorities.AttributeProviderAuthority;
import it.smartcommunitylab.aac.core.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.AttributeConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.config.AttributeProviderConfig;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableAttributeProvider;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

public abstract class AbstractAttributeAuthority<
    S extends AttributeProvider<M, C>, M extends AbstractConfigMap, C extends AbstractAttributeProviderConfig<M>
>
    extends AbstractConfigurableProviderAuthority<S, UserAttributes, ConfigurableAttributeProvider, M, C>
    implements AttributeProviderAuthority<S, M, C>, InitializingBean {

    // attributes sets service
    protected final AttributeService attributeService;

    // configuration provider
    protected AttributeConfigurationProvider<M, C> configProvider;

    public AbstractAttributeAuthority(
        String authorityId,
        AttributeService attributeService,
        ProviderConfigRepository<C> registrationRepository
    ) {
        super(authorityId, registrationRepository);
        Assert.notNull(attributeService, "attribute service is mandatory");

        this.attributeService = attributeService;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

    @Override
    public AttributeConfigurationProvider<M, C> getConfigurationProvider() {
        return configProvider;
    }

    @Autowired
    public void setConfigProvider(AttributeConfigurationProvider<M, C> configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(configProvider, "config provider is mandatory");
    }
}
