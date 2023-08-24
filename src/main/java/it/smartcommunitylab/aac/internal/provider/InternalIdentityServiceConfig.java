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
import it.smartcommunitylab.aac.accounts.provider.AccountServiceSettingsMap;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityServiceConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityService;

public class InternalIdentityServiceConfig extends AbstractIdentityServiceConfig<InternalIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_IDENTITY_SERVICE +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_INTERNAL;

    public InternalIdentityServiceConfig(String provider, String realm) {
        super(
            SystemKeys.AUTHORITY_INTERNAL,
            provider,
            realm,
            new AccountServiceSettingsMap(),
            new InternalIdentityProviderConfigMap()
        );
    }

    public InternalIdentityServiceConfig(
        ConfigurableIdentityService cp,
        AccountServiceSettingsMap settingsMap,
        InternalIdentityProviderConfigMap configMap
    ) {
        super(cp, settingsMap, configMap);
    }

    @Override
    public String getRepositoryId() {
        // not configurable for now
        return getRealm();
    }

    /*
     * config flags
     */
    public boolean isEnableRegistration() {
        return configMap.getEnableRegistration() != null ? configMap.getEnableRegistration().booleanValue() : true;
    }

    public boolean isEnableUpdate() {
        return configMap.getEnableUpdate() != null ? configMap.getEnableUpdate().booleanValue() : true;
    }

    public boolean isEnableDelete() {
        return configMap.getEnableDelete() != null ? configMap.getEnableDelete().booleanValue() : true;
    }

    public boolean isConfirmationRequired() {
        return configMap.getConfirmationRequired() != null ? configMap.getConfirmationRequired().booleanValue() : true;
    }
}
