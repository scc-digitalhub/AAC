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

package it.smartcommunitylab.aac.openid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableIdentityProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OIDCIdentityConfigurationProvider
    extends AbstractIdentityConfigurationProvider<OIDCIdentityProviderConfigMap, OIDCIdentityProviderConfig> {

    @Autowired
    public OIDCIdentityConfigurationProvider(IdentityAuthoritiesProperties authoritiesProperties) {
        this(SystemKeys.AUTHORITY_OIDC, new OIDCIdentityProviderConfigMap());
        if (authoritiesProperties != null && authoritiesProperties.getOidc() != null) {
            setDefaultConfigMap(authoritiesProperties.getOidc());
        }
    }

    public OIDCIdentityConfigurationProvider(String authority, OIDCIdentityProviderConfigMap configMap) {
        super(authority);
        setDefaultConfigMap(configMap);
    }

    @Override
    protected OIDCIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        return new OIDCIdentityProviderConfig(cp, getConfigMap(cp.getConfiguration()));
    }
}
