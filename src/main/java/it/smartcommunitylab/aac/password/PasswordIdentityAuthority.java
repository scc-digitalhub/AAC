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

package it.smartcommunitylab.aac.password;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderAuthority;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.password.provider.PasswordFilterProvider;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityConfigurationProvider;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProvider;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfigMap;
import it.smartcommunitylab.aac.password.service.InternalPasswordUserCredentialsService;
import it.smartcommunitylab.aac.utils.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class PasswordIdentityAuthority
    extends AbstractIdentityProviderAuthority<PasswordIdentityProvider, InternalUserIdentity, PasswordIdentityProviderConfigMap, PasswordIdentityProviderConfig> {

    public static final String AUTHORITY_URL = "/auth/password/";

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;

    // password service
    private final InternalPasswordUserCredentialsService passwordService;

    // filter provider
    private final PasswordFilterProvider filterProvider;

    // services
    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;
    private ResourceEntityService resourceService;

    public PasswordIdentityAuthority(
        UserAccountService<InternalUserAccount> userAccountService,
        InternalPasswordUserCredentialsService passwordService,
        ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_PASSWORD, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(passwordService, "password service is mandatory");

        this.accountService = userAccountService;
        this.passwordService = passwordService;

        // build filter provider
        this.filterProvider = new PasswordFilterProvider(userAccountService, passwordService, registrationRepository);
    }

    @Autowired
    public void setConfigProvider(PasswordIdentityConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Autowired
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    @Autowired
    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public PasswordIdentityProvider buildProvider(PasswordIdentityProviderConfig config) {
        PasswordIdentityProvider idp = new PasswordIdentityProvider(
            config.getProvider(),
            accountService,
            passwordService,
            config,
            config.getRealm()
        );

        // set services
        idp.setMailService(mailService);
        idp.setUriBuilder(uriBuilder);
        idp.setResourceService(resourceService);

        return idp;
    }

    @Override
    public PasswordFilterProvider getFilterProvider() {
        return filterProvider;
    }
}
