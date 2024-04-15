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

package it.smartcommunitylab.aac.internal;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.provider.AccountServiceSettingsMap;
import it.smartcommunitylab.aac.base.authorities.AbstractProviderAuthority;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.credentials.CredentialsServiceAuthority;
import it.smartcommunitylab.aac.identity.IdentityServiceAuthority;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.internal.model.InternalEditableUserAccount;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityService;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityServiceConfig;
import it.smartcommunitylab.aac.users.service.UserEntityService;
import java.util.Collection;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class InternalIdentityServiceAuthority
    extends AbstractProviderAuthority<InternalIdentityService, InternalIdentityServiceConfig>
    implements
        IdentityServiceAuthority<
            InternalIdentityService,
            InternalUserIdentity,
            InternalUserAccount,
            InternalEditableUserAccount,
            InternalIdentityProviderConfigMap,
            InternalIdentityServiceConfig
        > {

    public static final String AUTHORITY_URL = "/auth/internal/";

    // internal authorities
    private final InternalAccountServiceAuthority accountServiceAuthority;
    private final Collection<CredentialsServiceAuthority<?, ?, ?, ?, ?>> credentialsServiceAuthorities;

    // services
    private final UserEntityService userEntityService;

    public InternalIdentityServiceAuthority(
        UserEntityService userEntityService,
        InternalAccountServiceAuthority accountServiceAuthority,
        Collection<CredentialsServiceAuthority<?, ?, ?, ?, ?>> credentialsServiceAuthorities,
        ProviderConfigRepository<InternalIdentityProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_INTERNAL, new InternalConfigTranslatorRepository(registrationRepository));
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(accountServiceAuthority, "account service authority is mandatory");

        this.accountServiceAuthority = accountServiceAuthority;
        this.credentialsServiceAuthorities = credentialsServiceAuthorities;

        this.userEntityService = userEntityService;
    }

    // @Override
    // public String getType() {
    //     return SystemKeys.RESOURCE_IDENTITY;
    // }

    protected InternalIdentityService buildProvider(InternalIdentityServiceConfig config) {
        InternalIdentityService idp = new InternalIdentityService(
            config.getProvider(),
            userEntityService,
            config,
            config.getRealm()
        );

        idp.setAccountServiceAuthority(accountServiceAuthority);
        idp.setCredentialsServiceAuthorities(credentialsServiceAuthorities);
        return idp;
    }

    @Override
    public FilterProvider getFilterProvider() {
        // TODO add filters for registration and for credentials management
        return null;
    }

    static class InternalConfigTranslatorRepository
        extends TranslatorProviderConfigRepository<InternalIdentityProviderConfig, InternalIdentityServiceConfig> {

        public InternalConfigTranslatorRepository(
            ProviderConfigRepository<InternalIdentityProviderConfig> externalRepository
        ) {
            super(externalRepository);
            setConverter(source -> {
                InternalIdentityServiceConfig config = new InternalIdentityServiceConfig(
                    source.getProvider(),
                    source.getRealm()
                );
                config.setName(source.getName());
                config.setTitleMap(source.getTitleMap());
                config.setDescriptionMap(source.getDescriptionMap());

                // we share the same configMap
                config.setConfigMap(source.getConfigMap());
                config.setVersion(source.getVersion());

                //build new settingsMap
                AccountServiceSettingsMap settingsMap = new AccountServiceSettingsMap();
                settingsMap.setPersistence(source.getPersistence());
                settingsMap.setRepositoryId(source.getRepositoryId());
                config.setSettingsMap(settingsMap);

                return config;
            });
        }
    }

    @Override
    public ConfigurationProvider<
        InternalIdentityServiceConfig,
        ConfigurableIdentityService,
        AccountServiceSettingsMap,
        InternalIdentityProviderConfigMap
    > getConfigurationProvider() {
        return null;
    }
}
