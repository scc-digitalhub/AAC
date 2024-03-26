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

package it.smartcommunitylab.aac.openidfed.cie.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.base.AbstractAccountServiceConfig;
import it.smartcommunitylab.aac.accounts.model.ConfigurableAccountService;
import it.smartcommunitylab.aac.accounts.provider.AccountServiceSettingsMap;

public class CieAccountServiceConfig extends AbstractAccountServiceConfig<CieIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_CIE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_ACCOUNT_SERVICE +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_CIE;

    public CieAccountServiceConfig(String provider, String realm) {
        this(SystemKeys.AUTHORITY_CIE, provider, realm);
    }

    public CieAccountServiceConfig(String authority, String provider, String realm) {
        super(authority, provider, realm, new AccountServiceSettingsMap(), new CieIdentityProviderConfigMap());
    }

    public CieAccountServiceConfig(
        ConfigurableAccountService cp,
        AccountServiceSettingsMap settingsMap,
        CieIdentityProviderConfigMap configMap
    ) {
        super(cp, settingsMap, configMap);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */

    @SuppressWarnings("unused")
    private CieAccountServiceConfig() {
        super();
    }

    @Override
    public String getRepositoryId() {
        // not configurable for now
        return getProvider();
    }
}
