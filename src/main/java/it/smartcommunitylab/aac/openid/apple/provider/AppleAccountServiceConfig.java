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
import it.smartcommunitylab.aac.base.provider.config.AbstractAccountServiceConfig;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableAccountProvider;

public class AppleAccountServiceConfig extends AbstractAccountServiceConfig<AppleIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_APPLE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_ACCOUNT_SERVICE +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_APPLE;

    public AppleAccountServiceConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_APPLE, provider, realm, new AppleIdentityProviderConfigMap());
    }

    public AppleAccountServiceConfig(ConfigurableAccountProvider cp, AppleIdentityProviderConfigMap configMap) {
        super(cp, configMap);
    }

    public String getRepositoryId() {
        // not configurable for now
        return getProvider();
    }
}
