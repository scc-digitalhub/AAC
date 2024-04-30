/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.service;

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
import it.smartcommunitylab.aac.saml.model.SamlEditableUserAccount;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import it.smartcommunitylab.aac.spid.provider.SpidAccountService;
import it.smartcommunitylab.aac.spid.provider.SpidAccountServiceConfig;
import it.smartcommunitylab.aac.spid.provider.SpidAccountServiceConfigConverter;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class SpidAccountServiceAuthority
    extends AbstractProviderAuthority<SpidAccountService, SpidAccountServiceConfig>
    implements
        AccountServiceAuthority<SpidAccountService, SamlUserAccount, SamlEditableUserAccount, SpidAccountServiceConfig, SpidIdentityProviderConfigMap> {

    private final UserAccountService<SamlUserAccount> accountService;

    private ResourceEntityService resourceService;

    public SpidAccountServiceAuthority(
        String authorityId,
        UserAccountService<SamlUserAccount> userAccountService,
        ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository
    ) {
        super(authorityId, new SpidConfigTranslatorRepository(registrationRepository));
        Assert.notNull(userAccountService, "account service is mandatory");
        this.accountService = userAccountService;
    }

    @Autowired
    public SpidAccountServiceAuthority(
        UserAccountService<SamlUserAccount> userAccountService,
        ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository
    ) {
        this(SystemKeys.AUTHORITY_SPID, userAccountService, registrationRepository);
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    protected SpidAccountService buildProvider(SpidAccountServiceConfig config) {
        SpidAccountService service = new SpidAccountService(
            config.getProvider(),
            accountService,
            config,
            config.getRealm()
        );
        service.setResourceService(resourceService);
        return service;
    }

    @Override
    public ConfigurationProvider<SpidAccountServiceConfig, ConfigurableAccountService, AccountServiceSettingsMap, SpidIdentityProviderConfigMap> getConfigurationProvider() {
        return null;
    }

    static class SpidConfigTranslatorRepository
        extends TranslatorProviderConfigRepository<SpidIdentityProviderConfig, SpidAccountServiceConfig> {

        public SpidConfigTranslatorRepository(ProviderConfigRepository<SpidIdentityProviderConfig> externalRepository) {
            super(externalRepository);
            setConverter(new SpidAccountServiceConfigConverter());
        }
    }
}
