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

package it.smartcommunitylab.aac.openid.apple;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.provider.UserAccountService;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.identity.base.AbstractSingleProviderIdentityAuthority;
import it.smartcommunitylab.aac.openid.apple.auth.AppleClientRegistrationRepository;
import it.smartcommunitylab.aac.openid.apple.provider.AppleFilterProvider;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityConfigurationProvider;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProvider;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openid.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class AppleIdentityAuthority
    extends AbstractSingleProviderIdentityAuthority<AppleIdentityProvider, OIDCUserIdentity, AppleIdentityProviderConfigMap, AppleIdentityProviderConfig> {

    public static final String AUTHORITY_URL = "/auth/" + SystemKeys.AUTHORITY_APPLE + "/";

    // oidc account service
    private final UserAccountService<OIDCUserAccount> accountService;

    // filter provider
    private final AppleFilterProvider filterProvider;

    // oauth shared services
    private final AppleClientRegistrationRepository clientRegistrationRepository;

    // execution service for custom attributes mapping
    private ScriptExecutionService executionService;
    private ResourceEntityService resourceService;

    public AppleIdentityAuthority(
        UserAccountService<OIDCUserAccount> userAccountService,
        ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_APPLE, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");

        this.accountService = userAccountService;
        this.clientRegistrationRepository = new AppleClientRegistrationRepository(registrationRepository);

        // build filter provider
        this.filterProvider = new AppleFilterProvider(clientRegistrationRepository, registrationRepository);
    }

    @Autowired
    public void setConfigProvider(AppleIdentityConfigurationProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Autowired
    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }

    @Override
    public AppleFilterProvider getFilterProvider() {
        return this.filterProvider;
    }

    @Override
    public AppleIdentityProvider buildProvider(AppleIdentityProviderConfig config) {
        String id = config.getProvider();

        AppleIdentityProvider idp = new AppleIdentityProvider(id, accountService, config, config.getRealm());

        idp.setExecutionService(executionService);
        idp.setResourceService(resourceService);

        return idp;
    }
}
