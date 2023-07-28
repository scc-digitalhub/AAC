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

package it.smartcommunitylab.aac.saml;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.authorities.AbstractProviderAuthority;
import it.smartcommunitylab.aac.base.model.AbstractEditableAccount;
import it.smartcommunitylab.aac.core.authorities.AccountServiceAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.saml.provider.SamlAccountService;
import it.smartcommunitylab.aac.saml.provider.SamlAccountServiceConfig;
import it.smartcommunitylab.aac.saml.provider.SamlAccountServiceConfigConverter;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfigMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class SamlAccountServiceAuthority
    extends AbstractProviderAuthority<SamlAccountService, SamlUserAccount, ConfigurableAccountProvider, SamlIdentityProviderConfigMap, SamlAccountServiceConfig>
    implements
        AccountServiceAuthority<SamlAccountService, SamlUserAccount, AbstractEditableAccount, SamlIdentityProviderConfigMap, SamlAccountServiceConfig> {

    // account service
    private final UserAccountService<SamlUserAccount> accountService;
    private ResourceEntityService resourceService;

    @Autowired
    public SamlAccountServiceAuthority(
        UserAccountService<SamlUserAccount> userAccountService,
        ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository
    ) {
        this(SystemKeys.AUTHORITY_SAML, userAccountService, registrationRepository);
    }

    public SamlAccountServiceAuthority(
        String authority,
        UserAccountService<SamlUserAccount> userAccountService,
        ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository
    ) {
        super(authority, new SamlConfigTranslatorRepository(registrationRepository));
        Assert.notNull(userAccountService, "account service is mandatory");

        this.accountService = userAccountService;
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    protected SamlAccountService buildProvider(SamlAccountServiceConfig config) {
        SamlAccountService service = new SamlAccountService(
            config.getProvider(),
            accountService,
            config,
            config.getRealm()
        );
        service.setResourceService(resourceService);

        return service;
    }

    static class SamlConfigTranslatorRepository
        extends TranslatorProviderConfigRepository<SamlIdentityProviderConfig, SamlAccountServiceConfig> {

        public SamlConfigTranslatorRepository(ProviderConfigRepository<SamlIdentityProviderConfig> externalRepository) {
            super(externalRepository);
            setConverter(new SamlAccountServiceConfigConverter());
        }
    }
}
