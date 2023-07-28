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

package it.smartcommunitylab.aac.openid.apple.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableIdentityProvider;

import org.springframework.stereotype.Service;

@Service
public class AppleIdentityConfigurationProvider
    extends AbstractIdentityConfigurationProvider<AppleIdentityProviderConfigMap, AppleIdentityProviderConfig> {

    public AppleIdentityConfigurationProvider(IdentityAuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_APPLE);
        if (authoritiesProperties != null && authoritiesProperties.getApple() != null) {
            setDefaultConfigMap(authoritiesProperties.getApple());
        } else {
            setDefaultConfigMap(new AppleIdentityProviderConfigMap());
        }
    }

    @Override
    protected AppleIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        return new AppleIdentityProviderConfig(cp, getConfigMap(cp.getConfiguration()));
    }
}
