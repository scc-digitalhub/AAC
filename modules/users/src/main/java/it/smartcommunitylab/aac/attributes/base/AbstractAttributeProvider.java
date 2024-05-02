/**
 * Copyright 2023 Fondazione Bruno Kessler
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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.provider.AttributeProvider;
import it.smartcommunitylab.aac.attributes.provider.AttributeProviderSettingsMap;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.provider.AbstractConfigurableResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Transactional
public abstract class AbstractAttributeProvider<
    U extends AbstractUserAttributes, C extends AbstractAttributeProviderConfig<M>, M extends AbstractConfigMap
>
    extends AbstractConfigurableResourceProvider<U, C, AttributeProviderSettingsMap, M>
    implements AttributeProvider<U, M, C>, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected AbstractAttributeProvider(
        String authority,
        String providerId,
        AttributeService attributeService,
        C config,
        String realm
    ) {
        super(authority, providerId, realm, config);
        Assert.notNull(config, "provider config is mandatory");
        Assert.notNull(attributeService, "attribute service is mandatory");
    }

    @Override
    public void afterPropertiesSet() throws Exception {}

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }
}
