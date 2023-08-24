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

package it.smartcommunitylab.aac.accounts.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.model.ConfigurableAccountService;
import it.smartcommunitylab.aac.accounts.provider.AccountServiceConfig;
import it.smartcommunitylab.aac.accounts.provider.AccountServiceSettingsMap;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.provider.config.AbstractProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalAccountServiceConfig;
import it.smartcommunitylab.aac.model.PersistenceMode;
import it.smartcommunitylab.aac.oidc.apple.provider.AppleAccountServiceConfig;
import it.smartcommunitylab.aac.oidc.provider.OIDCAccountServiceConfig;
import it.smartcommunitylab.aac.saml.provider.SamlAccountServiceConfig;
import org.springframework.util.StringUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    {
        @Type(value = InternalAccountServiceConfig.class, name = InternalAccountServiceConfig.RESOURCE_TYPE),
        @Type(value = AppleAccountServiceConfig.class, name = AppleAccountServiceConfig.RESOURCE_TYPE),
        @Type(value = OIDCAccountServiceConfig.class, name = OIDCAccountServiceConfig.RESOURCE_TYPE),
        @Type(value = SamlAccountServiceConfig.class, name = SamlAccountServiceConfig.RESOURCE_TYPE),
    }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.ALWAYS)
public abstract class AbstractAccountServiceConfig<M extends AbstractConfigMap>
    extends AbstractProviderConfig<AccountServiceSettingsMap, M>
    implements AccountServiceConfig<M> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected AbstractAccountServiceConfig(
        String authority,
        String provider,
        String realm,
        AccountServiceSettingsMap settingsMap,
        M configMap
    ) {
        super(authority, provider, realm, settingsMap, configMap);
    }

    protected AbstractAccountServiceConfig(
        ConfigurableAccountService cp,
        AccountServiceSettingsMap settingsMap,
        M configMap
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
    protected AbstractAccountServiceConfig() {
        super();
    }

    public String getRepositoryId() {
        // if undefined always use realm as default repository id
        return StringUtils.hasText(settingsMap.getRepositoryId()) ? settingsMap.getRepositoryId() : getRealm();
    }

    public PersistenceMode getPersistence() {
        // by default persist to repository
        return settingsMap.getPersistence() != null ? settingsMap.getPersistence() : PersistenceMode.REPOSITORY;
    }
}
