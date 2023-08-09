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
import it.smartcommunitylab.aac.accounts.provider.UserAccountService;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.identity.base.AbstractSingleProviderIdentityAuthority;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityFilterProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigurationProvider;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class InternalIdentityProviderAuthority
    extends AbstractSingleProviderIdentityAuthority<InternalIdentityProvider, InternalUserIdentity, InternalIdentityProviderConfigMap, InternalIdentityProviderConfig>
    implements InitializingBean {

    public static final String AUTHORITY_URL = "/auth/internal/";

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;
    private final InternalUserConfirmKeyService confirmKeyService;

    // filter provider
    private final InternalIdentityFilterProvider filterProvider;

    //resource service for accounts
    private ResourceEntityService resourceService;

    public InternalIdentityProviderAuthority(
        UserAccountService<InternalUserAccount> userAccountService,
        InternalUserConfirmKeyService confirmKeyService,
        ProviderConfigRepository<InternalIdentityProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_INTERNAL, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(confirmKeyService, "confirm key service is mandatory");
        Assert.notNull(registrationRepository, "config repository is mandatory");

        this.accountService = userAccountService;
        this.confirmKeyService = confirmKeyService;

        // build filter provider
        this.filterProvider =
            new InternalIdentityFilterProvider(userAccountService, confirmKeyService, registrationRepository);
    }

    @Autowired
    public void setConfigProvider(InternalIdentityProviderConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    protected InternalIdentityProvider buildProvider(InternalIdentityProviderConfig config) {
        InternalIdentityProvider idp = new InternalIdentityProvider(
            config.getProvider(),
            accountService,
            confirmKeyService,
            config,
            config.getRealm()
        );

        idp.setResourceService(resourceService);
        return idp;
    }

    @Override
    public InternalIdentityFilterProvider getFilterProvider() {
        return filterProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Assert.notNull(getConfigurationProvider(), "config provider is mandatory");
    }
}
