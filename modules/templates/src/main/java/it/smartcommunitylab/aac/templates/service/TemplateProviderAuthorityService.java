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

package it.smartcommunitylab.aac.templates.service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.service.AbstractConfigurableAuthorityService;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.templates.TemplateProviderAuthority;
import it.smartcommunitylab.aac.templates.model.Template;
import it.smartcommunitylab.aac.templates.provider.TemplateProvider;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderConfig;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderSettingsMap;
import java.util.Collection;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class TemplateProviderAuthorityService
    extends AbstractConfigurableAuthorityService<
        TemplateProviderAuthority<
            ? extends TemplateProvider<
                ? extends Template,
                ? extends ConfigMap,
                ? extends TemplateProviderConfig<? extends ConfigMap>
            >,
            ? extends Template,
            ? extends TemplateProviderConfig<? extends ConfigMap>,
            ? extends ConfigMap
        >,
        TemplateProviderSettingsMap
    >
    implements InitializingBean {

    public TemplateProviderAuthorityService(
        Collection<
            TemplateProviderAuthority<
                ? extends TemplateProvider<
                    ? extends Template,
                    ? extends ConfigMap,
                    ? extends TemplateProviderConfig<? extends ConfigMap>
                >,
                ? extends Template,
                ? extends TemplateProviderConfig<? extends ConfigMap>,
                ? extends ConfigMap
            >
        > authorities
    ) {
        super(SystemKeys.RESOURCE_TEMPLATE);
        this.setAuthorities(authorities);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notEmpty(authorities, "at least one provider authority is required");
    }
}
