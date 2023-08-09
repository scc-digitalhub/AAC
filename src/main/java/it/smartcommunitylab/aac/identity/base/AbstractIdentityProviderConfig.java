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

package it.smartcommunitylab.aac.identity.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.provider.config.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderConfig;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.model.PersistenceMode;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import java.util.Collections;
import java.util.Map;
import org.springframework.util.StringUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    {
        @Type(value = InternalIdentityProviderConfig.class, name = InternalIdentityProviderConfig.RESOURCE_TYPE),
        @Type(value = AppleIdentityProviderConfig.class, name = AppleIdentityProviderConfig.RESOURCE_TYPE),
        @Type(value = OIDCIdentityProviderConfig.class, name = OIDCIdentityProviderConfig.RESOURCE_TYPE),
        @Type(value = PasswordIdentityProviderConfig.class, name = PasswordIdentityProviderConfig.RESOURCE_TYPE),
        @Type(value = SamlIdentityProviderConfig.class, name = SamlIdentityProviderConfig.RESOURCE_TYPE),
        @Type(value = WebAuthnIdentityProviderConfig.class, name = WebAuthnIdentityProviderConfig.RESOURCE_TYPE),
    }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.ALWAYS)
public abstract class AbstractIdentityProviderConfig<M extends AbstractConfigMap>
    extends AbstractProviderConfig<IdentityProviderSettingsMap, M>
    implements IdentityProviderConfig<M> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected AbstractIdentityProviderConfig(
        String authority,
        String provider,
        String realm,
        IdentityProviderSettingsMap settingsMap,
        M configMap
    ) {
        super(authority, provider, realm, settingsMap, configMap);
    }

    protected AbstractIdentityProviderConfig(
        ConfigurableProvider cp,
        IdentityProviderSettingsMap settingsMap,
        M configMap
    ) {
        super(cp, settingsMap, configMap);
    }
}
