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

package it.smartcommunitylab.aac.oidc.apple;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.AccountServiceAuthority;
import it.smartcommunitylab.aac.accounts.model.ConfigurableAccountService;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.accounts.provider.AccountServiceSettingsMap;
import it.smartcommunitylab.aac.base.authorities.AbstractProviderAuthority;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.oidc.apple.provider.AppleAccountService;
import it.smartcommunitylab.aac.oidc.apple.provider.AppleAccountServiceConfig;
import it.smartcommunitylab.aac.oidc.apple.provider.AppleAccountServiceConfigConverter;
import it.smartcommunitylab.aac.oidc.apple.provider.AppleIdentityProviderConfig;
import it.smartcommunitylab.aac.oidc.apple.provider.AppleIdentityProviderConfigMap;
import it.smartcommunitylab.aac.oidc.model.OIDCUserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class AppleAccountServiceAuthority
    extends AbstractProviderAuthority<AppleAccountService, AppleAccountServiceConfig>
    implements AccountServiceAuthority<AppleAccountService, AppleAccountServiceConfig, AppleIdentityProviderConfigMap> {

    // account service
    private final UserAccountService<OIDCUserAccount> accountService;
    private ResourceEntityService resourceService;

    public AppleAccountServiceAuthority(
        UserAccountService<OIDCUserAccount> userAccountService,
        ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_APPLE, new AppleConfigTranslatorRepository(registrationRepository));
        Assert.notNull(userAccountService, "account service is mandatory");

        this.accountService = userAccountService;
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    // @Override
    // public String getType() {
    //     return SystemKeys.RESOURCE_ACCOUNT;
    // }

    protected AppleAccountService buildProvider(AppleAccountServiceConfig config) {
        AppleAccountService service = new AppleAccountService(
            config.getProvider(),
            accountService,
            config,
            config.getRealm()
        );
        service.setResourceService(resourceService);

        return service;
    }

    static class AppleConfigTranslatorRepository
        extends TranslatorProviderConfigRepository<AppleIdentityProviderConfig, AppleAccountServiceConfig> {

        public AppleConfigTranslatorRepository(
            ProviderConfigRepository<AppleIdentityProviderConfig> externalRepository
        ) {
            super(externalRepository);
            setConverter(new AppleAccountServiceConfigConverter());
        }
    }

    @Override
    public ConfigurationProvider<AppleAccountServiceConfig, ConfigurableAccountService, AccountServiceSettingsMap, AppleIdentityProviderConfigMap> getConfigurationProvider() {
        return null;
    }
}
