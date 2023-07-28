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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.AbstractCredentialsServiceConfig;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableCredentialsProvider;

public class PasswordCredentialsServiceConfig
    extends AbstractCredentialsServiceConfig<PasswordIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_CREDENTIALS_SERVICE +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_PASSWORD;

    private static final int MIN_DURATION = 300;
    private static final int PASSWORD_MIN_LENGTH = 2;
    private static final int PASSWORD_MAX_LENGTH = 75;

    public PasswordCredentialsServiceConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, provider, realm, new PasswordIdentityProviderConfigMap());
    }

    public PasswordCredentialsServiceConfig(
        ConfigurableCredentialsProvider cp,
        PasswordIdentityProviderConfigMap configMap
    ) {
        super(cp, configMap);
    }

    public String getRepositoryId() {
        return configMap.getRepositoryId() != null ? configMap.getRepositoryId() : getRealm();
    }

    /*
     * config flags
     */
    public boolean isRequireAccountConfirmation() {
        return configMap.getRequireAccountConfirmation() != null
            ? configMap.getRequireAccountConfirmation().booleanValue()
            : true;
    }

    public boolean isEnablePasswordReset() {
        return configMap.getEnablePasswordReset() != null ? configMap.getEnablePasswordReset().booleanValue() : true;
    }

    public boolean isPasswordRequireAlpha() {
        return configMap.getPasswordRequireAlpha() != null ? configMap.getPasswordRequireAlpha().booleanValue() : false;
    }

    public boolean isPasswordRequireUppercaseAlpha() {
        return configMap.getPasswordRequireUppercaseAlpha() != null
            ? configMap.getPasswordRequireUppercaseAlpha().booleanValue()
            : false;
    }

    public boolean isPasswordRequireNumber() {
        return configMap.getPasswordRequireNumber() != null
            ? configMap.getPasswordRequireNumber().booleanValue()
            : false;
    }

    public boolean isPasswordRequireSpecial() {
        return configMap.getPasswordRequireSpecial() != null
            ? configMap.getPasswordRequireSpecial().booleanValue()
            : false;
    }

    public boolean isPasswordSupportWhitespace() {
        return configMap.getPasswordSupportWhitespace() != null
            ? configMap.getPasswordSupportWhitespace().booleanValue()
            : false;
    }

    /*
     * default config
     */

    public int getPasswordResetValidity() {
        return configMap.getPasswordResetValidity() != null
            ? configMap.getPasswordResetValidity().intValue()
            : MIN_DURATION;
    }

    public int getPasswordMinLength() {
        return configMap.getPasswordMinLength() != null
            ? configMap.getPasswordMinLength().intValue()
            : PASSWORD_MIN_LENGTH;
    }

    public int getPasswordMaxLength() {
        return configMap.getPasswordMaxLength() != null
            ? configMap.getPasswordMaxLength().intValue()
            : PASSWORD_MAX_LENGTH;
    }

    public int getPasswordKeepNumber() {
        return configMap.getPasswordKeepNumber() != null ? configMap.getPasswordKeepNumber().intValue() : 0;
    }

    public int getPasswordMaxDays() {
        return configMap.getPasswordMaxDays() != null ? configMap.getPasswordMaxDays().intValue() : -1;
    }
}
