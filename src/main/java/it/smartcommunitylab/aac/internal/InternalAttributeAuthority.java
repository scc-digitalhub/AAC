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

package it.smartcommunitylab.aac.internal;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.base.authorities.AbstractSingleConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.authorities.AttributeProviderAuthority;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeConfigurationProvider;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeProviderConfigMap;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeService;
import it.smartcommunitylab.aac.internal.service.InternalAttributeEntityService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class InternalAttributeAuthority
    extends AbstractSingleConfigurableProviderAuthority<InternalAttributeService, UserAttributes, ConfigurableAttributeProvider, InternalAttributeProviderConfigMap, InternalAttributeProviderConfig>
    implements
        AttributeProviderAuthority<InternalAttributeService, InternalAttributeProviderConfigMap, InternalAttributeProviderConfig>,
        InitializingBean {

    // attributes sets service
    protected final AttributeService attributeService;

    // configuration provider
    protected InternalAttributeConfigurationProvider configProvider;

    private final InternalAttributeEntityService attributeEntityService;

    public InternalAttributeAuthority(
        AttributeService attributeService,
        InternalAttributeEntityService attributeEntityService,
        ProviderConfigRepository<InternalAttributeProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_INTERNAL, registrationRepository);
        Assert.notNull(attributeEntityService, "attribute entity service is mandatory");
        Assert.notNull(attributeService, "attribute service is mandatory");

        this.attributeService = attributeService;
        this.attributeEntityService = attributeEntityService;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

    @Override
    public InternalAttributeConfigurationProvider getConfigurationProvider() {
        return configProvider;
    }

    @Autowired
    public void setConfigProvider(InternalAttributeConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(configProvider, "config provider is mandatory");
    }

    @Override
    protected InternalAttributeService buildProvider(InternalAttributeProviderConfig config) {
        InternalAttributeService ap = new InternalAttributeService(
            config.getProvider(),
            attributeService,
            attributeEntityService,
            config,
            config.getRealm()
        );

        return ap;
    }
}
