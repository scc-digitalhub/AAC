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

package it.smartcommunitylab.aac.openid;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.AccountServiceAuthority;
import it.smartcommunitylab.aac.core.base.AbstractEditableAccount;
import it.smartcommunitylab.aac.core.base.AbstractProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.provider.OIDCAccountService;
import it.smartcommunitylab.aac.openid.provider.OIDCAccountServiceConfig;
import it.smartcommunitylab.aac.openid.provider.OIDCAccountServiceConfigConverter;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class OIDCAccountServiceAuthority
    extends AbstractProviderAuthority<OIDCAccountService, OIDCUserAccount, ConfigurableAccountProvider, OIDCIdentityProviderConfigMap, OIDCAccountServiceConfig>
    implements
        AccountServiceAuthority<OIDCAccountService, OIDCUserAccount, AbstractEditableAccount, OIDCIdentityProviderConfigMap, OIDCAccountServiceConfig> {

    // account service
    private final UserAccountService<OIDCUserAccount> accountService;
    private ResourceEntityService resourceService;

    @Autowired
    public OIDCAccountServiceAuthority(
        UserAccountService<OIDCUserAccount> userAccountService,
        ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository
    ) {
        this(SystemKeys.AUTHORITY_OIDC, userAccountService, registrationRepository);
    }

    public OIDCAccountServiceAuthority(
        String authority,
        UserAccountService<OIDCUserAccount> userAccountService,
        ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository
    ) {
        super(authority, new OIDCConfigTranslatorRepository(registrationRepository));
        Assert.notNull(userAccountService, "account service is mandatory");

        this.accountService = userAccountService;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    protected OIDCAccountService buildProvider(OIDCAccountServiceConfig config) {
        OIDCAccountService service = new OIDCAccountService(
            config.getAuthority(),
            config.getProvider(),
            accountService,
            config,
            config.getRealm()
        );
        service.setResourceService(resourceService);

        return service;
    }

    static class OIDCConfigTranslatorRepository
        extends TranslatorProviderConfigRepository<OIDCIdentityProviderConfig, OIDCAccountServiceConfig> {

        public OIDCConfigTranslatorRepository(ProviderConfigRepository<OIDCIdentityProviderConfig> externalRepository) {
            super(externalRepository);
            setConverter(new OIDCAccountServiceConfigConverter());
        }
    }
}