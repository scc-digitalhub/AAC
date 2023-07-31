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
import it.smartcommunitylab.aac.base.authorities.AbstractProviderAuthority;
import it.smartcommunitylab.aac.core.authorities.AccountServiceAuthority;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.internal.model.InternalEditableUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalAccountService;
import it.smartcommunitylab.aac.internal.provider.InternalAccountServiceConfig;
import it.smartcommunitylab.aac.internal.provider.InternalAccountServiceConfigConverter;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;
import it.smartcommunitylab.aac.users.service.UserEntityService;
import it.smartcommunitylab.aac.utils.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class InternalAccountServiceAuthority
    extends AbstractProviderAuthority<InternalAccountService, InternalUserAccount, ConfigurableAccountProvider, InternalIdentityProviderConfigMap, InternalAccountServiceConfig>
    implements
        AccountServiceAuthority<InternalAccountService, InternalUserAccount, InternalEditableUserAccount, InternalIdentityProviderConfigMap, InternalAccountServiceConfig> {

    public static final String AUTHORITY_URL = "/auth/internal/";

    // user service
    private final UserEntityService userEntityService;
    private final ResourceEntityService resourceService;

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;
    private final InternalUserConfirmKeyService confirmKeyService;

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    public InternalAccountServiceAuthority(
        UserEntityService userEntityService,
        ResourceEntityService resourceService,
        UserAccountService<InternalUserAccount> userAccountService,
        InternalUserConfirmKeyService confirmKeyService,
        ProviderConfigRepository<InternalIdentityProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_INTERNAL, new InternalConfigTranslatorRepository(registrationRepository));
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(resourceService, "resource service is mandatory");
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(confirmKeyService, "confirm key service is mandatory");

        this.userEntityService = userEntityService;
        this.resourceService = resourceService;
        this.accountService = userAccountService;
        this.confirmKeyService = confirmKeyService;
    }

    @Autowired
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    @Autowired
    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    protected InternalAccountService buildProvider(InternalAccountServiceConfig config) {
        InternalAccountService service = new InternalAccountService(
            config.getProvider(),
            userEntityService,
            accountService,
            confirmKeyService,
            config,
            config.getRealm()
        );

        service.setMailService(mailService);
        service.setUriBuilder(uriBuilder);
        service.setResourceService(resourceService);

        return service;
    }

    static class InternalConfigTranslatorRepository
        extends TranslatorProviderConfigRepository<InternalIdentityProviderConfig, InternalAccountServiceConfig> {

        public InternalConfigTranslatorRepository(
            ProviderConfigRepository<InternalIdentityProviderConfig> externalRepository
        ) {
            super(externalRepository);
            setConverter(new InternalAccountServiceConfigConverter());
        }
    }
}
