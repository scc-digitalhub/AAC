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

package it.smartcommunitylab.aac.openidfed;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderAuthority;
import it.smartcommunitylab.aac.oidc.model.OIDCUserAccount;
import it.smartcommunitylab.aac.oidc.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedFilterProvider;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityConfigurationProvider;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProvider;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfigMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class OpenIdFedIdentityAuthority
    extends AbstractIdentityProviderAuthority<OpenIdFedIdentityProvider, OIDCUserIdentity, OpenIdFedIdentityProviderConfig, OpenIdFedIdentityProviderConfigMap> {

    public static final String AUTHORITY_URL = "/auth/" + SystemKeys.AUTHORITY_OPENIDFED + "/";

    // oidc account service
    private final UserAccountService<OIDCUserAccount> accountService;

    // filter provider
    private final OpenIdFedFilterProvider filterProvider;

    // execution service for custom attributes mapping
    private ScriptExecutionService executionService;
    private ResourceEntityService resourceService;

    @Autowired
    public OpenIdFedIdentityAuthority(
        UserAccountService<OIDCUserAccount> userAccountService,
        ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository
    ) {
        this(SystemKeys.AUTHORITY_OPENIDFED, userAccountService, registrationRepository);
    }

    public OpenIdFedIdentityAuthority(
        String authorityId,
        UserAccountService<OIDCUserAccount> userAccountService,
        ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository
    ) {
        super(authorityId, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");

        this.accountService = userAccountService;

        // build filter provider
        this.filterProvider = new OpenIdFedFilterProvider(authorityId, registrationRepository);
    }

    @Autowired
    public void setConfigProvider(OpenIdFedIdentityConfigurationProvider configProvider) {
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

    @Autowired
    public void setRealmAwareUriBuilder(RealmAwareUriBuilder realmAwareUriBuilder) {
        this.filterProvider.setRealmAwareUriBuilder(realmAwareUriBuilder);
    }

    @Override
    public OpenIdFedFilterProvider getFilterProvider() {
        return this.filterProvider;
    }

    @Override
    public OpenIdFedIdentityProvider buildProvider(OpenIdFedIdentityProviderConfig config) {
        String id = config.getProvider();

        OpenIdFedIdentityProvider idp = new OpenIdFedIdentityProvider(
            authorityId,
            id,
            accountService,
            config,
            config.getRealm()
        );

        idp.setExecutionService(executionService);
        idp.setResourceService(resourceService);
        return idp;
    }
}
