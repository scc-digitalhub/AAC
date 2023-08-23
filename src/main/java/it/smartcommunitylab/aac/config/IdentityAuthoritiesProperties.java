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

package it.smartcommunitylab.aac.config;

import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfigMap;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfigMap;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfigMap;
import java.util.List;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class IdentityAuthoritiesProperties {

    // TODO add enable//disable flag on authorities

    @NestedConfigurationProperty
    IdentityProviderSettingsMap settings;

    @NestedConfigurationProperty
    private InternalIdentityProviderConfigMap internal;

    @NestedConfigurationProperty
    private PasswordIdentityProviderConfigMap password;

    @NestedConfigurationProperty
    private WebAuthnIdentityProviderConfigMap webauthn;

    @NestedConfigurationProperty
    private OIDCIdentityProviderConfigMap oidc;

    @NestedConfigurationProperty
    private SamlIdentityProviderConfigMap saml;

    @NestedConfigurationProperty
    private AppleIdentityProviderConfigMap apple;

    @NestedConfigurationProperty
    private List<CustomAuthoritiesProperties> custom;

    public IdentityProviderSettingsMap getSettings() {
        return settings;
    }

    public void setSettings(IdentityProviderSettingsMap settings) {
        this.settings = settings;
    }

    public InternalIdentityProviderConfigMap getInternal() {
        return internal;
    }

    public void setInternal(InternalIdentityProviderConfigMap internal) {
        this.internal = internal;
    }

    public PasswordIdentityProviderConfigMap getPassword() {
        return password;
    }

    public void setPassword(PasswordIdentityProviderConfigMap password) {
        this.password = password;
    }

    public WebAuthnIdentityProviderConfigMap getWebauthn() {
        return webauthn;
    }

    public void setWebauthn(WebAuthnIdentityProviderConfigMap webauthn) {
        this.webauthn = webauthn;
    }

    public OIDCIdentityProviderConfigMap getOidc() {
        return oidc;
    }

    public void setOidc(OIDCIdentityProviderConfigMap oidc) {
        this.oidc = oidc;
    }

    public SamlIdentityProviderConfigMap getSaml() {
        return saml;
    }

    public void setSaml(SamlIdentityProviderConfigMap saml) {
        this.saml = saml;
    }

    public AppleIdentityProviderConfigMap getApple() {
        return apple;
    }

    public void setApple(AppleIdentityProviderConfigMap apple) {
        this.apple = apple;
    }

    public List<CustomAuthoritiesProperties> getCustom() {
        return custom;
    }

    public void setCustom(List<CustomAuthoritiesProperties> custom) {
        this.custom = custom;
    }
}
