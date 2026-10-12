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

package it.smartcommunitylab.aac.eidas.service;

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
import it.smartcommunitylab.aac.eidas.provider.EIDASIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.model.SamlEditableUserAccount;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import it.smartcommunitylab.aac.spid.provider.SpidAccountService;
import it.smartcommunitylab.aac.spid.provider.SpidAccountServiceConfig;
import it.smartcommunitylab.aac.spid.provider.SpidAccountServiceConfigConverter;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/*
 * Exposes the eidas accounts to the platform.
 *
 * The identity provider creates and updates accounts on its own during login, but the console and
 * the user profile list accounts by walking the registered account service authorities: without
 * this bean an eidas account exists in the repository and is invisible everywhere else.
 *
 * Mirrors SpidAccountServiceAuthority on the eidas configurations: the account service configs are
 * derived with the SPID converter, which keeps the authority of the source (eidas), and the SPID
 * account service takes its authority from them.
 */
@Service
public class EIDASAccountServiceAuthority
    extends AbstractProviderAuthority<SpidAccountService, SpidAccountServiceConfig>
    implements
        AccountServiceAuthority<SpidAccountService, SamlUserAccount, SamlEditableUserAccount, SpidAccountServiceConfig, SpidIdentityProviderConfigMap> {

    private final UserAccountService<SamlUserAccount> accountService;

    private ResourceEntityService resourceService;

    @Autowired
    public EIDASAccountServiceAuthority(
        UserAccountService<SamlUserAccount> userAccountService,
        @Qualifier("eidasProviderConfigRepository") ProviderConfigRepository<EIDASIdentityProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_EIDAS, new EIDASConfigTranslatorRepository(registrationRepository));
        Assert.notNull(userAccountService, "account service is mandatory");
        this.accountService = userAccountService;
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
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

    static class EIDASConfigTranslatorRepository
        extends TranslatorProviderConfigRepository<EIDASIdentityProviderConfig, SpidAccountServiceConfig> {

        public EIDASConfigTranslatorRepository(
            ProviderConfigRepository<EIDASIdentityProviderConfig> externalRepository
        ) {
            super(externalRepository);
            SpidAccountServiceConfigConverter converter = new SpidAccountServiceConfigConverter();
            setConverter(converter::convert);
        }
    }
}
