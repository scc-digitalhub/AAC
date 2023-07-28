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

package it.smartcommunitylab.aac.password.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableIdentityProvider;

public class PasswordIdentityProviderConfig extends AbstractIdentityProviderConfig<PasswordIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + PasswordIdentityProviderConfigMap.RESOURCE_TYPE;

    private static final int DEFAULT_SESSION_DURATION = 24 * 60 * 60; // 24h
    private static final int DEFAULT_RESET_DURATION = 900; // 15m

    public PasswordIdentityProviderConfig(
        @JsonProperty("provider") String provider,
        @JsonProperty("realm") String realm
    ) {
        super(SystemKeys.AUTHORITY_PASSWORD, provider, realm, new PasswordIdentityProviderConfigMap());
    }

    public PasswordIdentityProviderConfig(
        ConfigurableIdentityProvider cp,
        PasswordIdentityProviderConfigMap configMap
    ) {
        super(cp, configMap);
    }

    public String getRepositoryId() {
        return configMap.getRepositoryId() != null ? configMap.getRepositoryId() : getRealm();
    }

    /*
     * display mode
     */

    public boolean displayAsButton() {
        return configMap.getDisplayAsButton() != null ? configMap.getDisplayAsButton().booleanValue() : false;
    }

    /*
     * default config
     */
    public boolean isEnablePasswordReset() {
        return configMap.getEnablePasswordReset() != null ? configMap.getEnablePasswordReset().booleanValue() : true;
    }

    public int getPasswordResetValidity() {
        return configMap.getPasswordResetValidity() != null
            ? configMap.getPasswordResetValidity().intValue()
            : DEFAULT_RESET_DURATION;
    }

    public int getMaxSessionDuration() {
        return configMap.getMaxSessionDuration() != null
            ? configMap.getMaxSessionDuration().intValue()
            : DEFAULT_SESSION_DURATION;
    }

    /*
     * Account confirmation
     */
    public boolean isRequireAccountConfirmation() {
        return configMap.getRequireAccountConfirmation() != null
            ? configMap.getRequireAccountConfirmation().booleanValue()
            : true;
    }
}
