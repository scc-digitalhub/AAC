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

package it.smartcommunitylab.aac.webauthn.provider;

import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserVerificationRequirement;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractCredentialsServiceConfig;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableCredentialsProvider;

public class WebAuthnCredentialsServiceConfig
    extends AbstractCredentialsServiceConfig<WebAuthnIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_CREDENTIALS_SERVICE +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_WEBAUTHN;

    private static final int DEFAULT_TIMEOUT = 30;

    public WebAuthnCredentialsServiceConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, provider, realm, new WebAuthnIdentityProviderConfigMap());
    }

    public WebAuthnCredentialsServiceConfig(
        ConfigurableCredentialsProvider cp,
        WebAuthnIdentityProviderConfigMap configMap
    ) {
        super(cp, configMap);
    }

    public String getRepositoryId() {
        // not configurable for now
        return getRealm();
    }

    /*
     * config flags
     */
    public boolean isRequireAccountConfirmation() {
        return configMap.getRequireAccountConfirmation() != null
            ? configMap.getRequireAccountConfirmation().booleanValue()
            : true;
    }

    public boolean isAllowedUnstrustedAttestation() {
        return configMap.getAllowUntrustedAttestation() != null
            ? configMap.getAllowUntrustedAttestation().booleanValue()
            : false;
    }

    public UserVerificationRequirement getRequireUserVerification() {
        return configMap.getRequireUserVerification() != null
            ? configMap.getRequireUserVerification()
            : UserVerificationRequirement.PREFERRED;
    }

    public ResidentKeyRequirement getRequireResidentKey() {
        return configMap.getRequireResidentKey() != null
            ? configMap.getRequireResidentKey()
            : ResidentKeyRequirement.PREFERRED;
    }

    public int getRegistrationTimeout() {
        // return timeout in seconds
        return configMap.getRegistrationTimeout() != null
            ? configMap.getRegistrationTimeout().intValue()
            : DEFAULT_TIMEOUT;
    }
}
