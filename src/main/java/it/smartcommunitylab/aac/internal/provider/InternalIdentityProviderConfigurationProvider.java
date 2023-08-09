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

package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import org.springframework.stereotype.Service;

@Service
public class InternalIdentityProviderConfigurationProvider
    extends AbstractIdentityConfigurationProvider<InternalIdentityProviderConfigMap, InternalIdentityProviderConfig> {

    public InternalIdentityProviderConfigurationProvider(IdentityAuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_INTERNAL);
        if (authoritiesProperties != null && authoritiesProperties.getInternal() != null) {
            setDefaultConfigMap(authoritiesProperties.getInternal());
        } else {
            setDefaultConfigMap(new InternalIdentityProviderConfigMap());
        }
    }

    @Override
    protected InternalIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        // build configMap and then config
        return new InternalIdentityProviderConfig(cp, getConfigMap(cp.getConfiguration()));
    }
}
