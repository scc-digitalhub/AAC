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

import com.yubico.webauthn.data.UserVerificationRequirement;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;

public class WebAuthnIdentityProviderConfig extends AbstractIdentityProviderConfig<WebAuthnIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + WebAuthnIdentityProviderConfigMap.RESOURCE_TYPE;

    private static final int TIMEOUT = 9;
    private static final int MAX_SESSION_DURATION = 24 * 60 * 60; // 24h

    public WebAuthnIdentityProviderConfig(String provider, String realm) {
        super(
            SystemKeys.AUTHORITY_WEBAUTHN,
            provider,
            realm,
            new IdentityProviderSettingsMap(),
            new WebAuthnIdentityProviderConfigMap()
        );
    }

    public WebAuthnIdentityProviderConfig(
        ConfigurableIdentityProvider cp,
        IdentityProviderSettingsMap settingsMap,
        WebAuthnIdentityProviderConfigMap configMap
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
    private WebAuthnIdentityProviderConfig() {
        super();
    }

    public String getRepositoryId() {
        // not configurable for now
        return getRealm();
    }

    /*
     * config flags
     */
    public int getMaxSessionDuration() {
        return configMap.getMaxSessionDuration() != null
            ? configMap.getMaxSessionDuration().intValue()
            : MAX_SESSION_DURATION;
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

    public int getLoginTimeout() {
        // return timeout in seconds
        return configMap.getLoginTimeout() != null ? configMap.getLoginTimeout().intValue() : TIMEOUT;
    }

    /*
     * display mode
     */

    public boolean displayAsButton() {
        return configMap.getDisplayAsButton() != null ? configMap.getDisplayAsButton().booleanValue() : false;
    }

    public boolean isRequireAccountConfirmation() {
        return configMap.getRequireAccountConfirmation() != null
            ? configMap.getRequireAccountConfirmation().booleanValue()
            : true;
    }
}
