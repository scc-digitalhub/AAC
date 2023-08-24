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

package it.smartcommunitylab.aac.oidc;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderAuthority;
import it.smartcommunitylab.aac.oidc.auth.OIDCClientRegistrationRepository;
import it.smartcommunitylab.aac.oidc.model.OIDCUserAccount;
import it.smartcommunitylab.aac.oidc.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.oidc.provider.OIDCFilterProvider;
import it.smartcommunitylab.aac.oidc.provider.OIDCIdentityConfigurationProvider;
import it.smartcommunitylab.aac.oidc.provider.OIDCIdentityProvider;
import it.smartcommunitylab.aac.oidc.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.oidc.provider.OIDCIdentityProviderConfigMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class OIDCIdentityAuthority
    extends AbstractIdentityProviderAuthority<OIDCIdentityProvider, OIDCUserIdentity, OIDCIdentityProviderConfigMap, OIDCIdentityProviderConfig> {

    public static final String AUTHORITY_URL = "/auth/" + SystemKeys.AUTHORITY_OIDC + "/";

    // oidc account service
    private final UserAccountService<OIDCUserAccount> accountService;

    // filter provider
    private final OIDCFilterProvider filterProvider;

    // oauth shared services
    private final OIDCClientRegistrationRepository clientRegistrationRepository;

    // execution service for custom attributes mapping
    private ScriptExecutionService executionService;
    private ResourceEntityService resourceService;

    @Autowired
    public OIDCIdentityAuthority(
        UserAccountService<OIDCUserAccount> userAccountService,
        ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository
    ) {
        this(SystemKeys.AUTHORITY_OIDC, userAccountService, registrationRepository);
    }

    public OIDCIdentityAuthority(
        String authorityId,
        UserAccountService<OIDCUserAccount> userAccountService,
        ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository
    ) {
        super(authorityId, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");

        this.accountService = userAccountService;
        this.clientRegistrationRepository = new OIDCClientRegistrationRepository(registrationRepository);

        // build filter provider
        this.filterProvider = new OIDCFilterProvider(authorityId, clientRegistrationRepository, registrationRepository);
    }

    @Autowired
    public void setConfigProvider(OIDCIdentityConfigurationProvider configProvider) {
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
    public OIDCFilterProvider getFilterProvider() {
        return this.filterProvider;
    }

    @Override
    public OIDCIdentityProvider buildProvider(OIDCIdentityProviderConfig config) {
        String id = config.getProvider();

        OIDCIdentityProvider idp = new OIDCIdentityProvider(authorityId, id, accountService, config, config.getRealm());

        idp.setExecutionService(executionService);
        idp.setResourceService(resourceService);
        return idp;
    }
}
