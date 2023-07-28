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

package it.smartcommunitylab.aac.saml.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableIdentityProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SamlIdentityConfigurationProvider
    extends AbstractIdentityConfigurationProvider<SamlIdentityProviderConfigMap, SamlIdentityProviderConfig> {

    @Autowired
    public SamlIdentityConfigurationProvider(IdentityAuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_SAML);
        if (authoritiesProperties != null && authoritiesProperties.getSaml() != null) {
            setDefaultConfigMap(authoritiesProperties.getSaml());
        } else {
            setDefaultConfigMap(new SamlIdentityProviderConfigMap());
        }
    }

    public SamlIdentityConfigurationProvider(String authority, SamlIdentityProviderConfigMap configMap) {
        super(authority);
        setDefaultConfigMap(configMap);
    }

    @Override
    protected SamlIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        return new SamlIdentityProviderConfig(cp, getConfigMap(cp.getConfiguration()));
    }
}
