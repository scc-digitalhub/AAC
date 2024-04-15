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
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderAuthority;
import it.smartcommunitylab.aac.saml.auth.SamlRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import it.smartcommunitylab.aac.saml.model.SamlUserIdentity;
import it.smartcommunitylab.aac.saml.provider.SamlFilterProvider;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityConfigurationProvider;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProvider;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfigMap;
import it.smartcommunitylab.aac.users.service.UserEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class SamlIdentityAuthority
    extends AbstractIdentityProviderAuthority<
        SamlIdentityProvider,
        SamlUserIdentity,
        SamlIdentityProviderConfig,
        SamlIdentityProviderConfigMap
    >
    implements ApplicationEventPublisherAware {

    public static final String AUTHORITY_URL = "/auth/" + SystemKeys.AUTHORITY_SAML + "/";

    //user entity service
    private final UserEntityService userEntityService;

    // saml account service
    private final UserAccountService<SamlUserAccount> accountService;

    // filter provider
    private final SamlFilterProvider filterProvider;

    // saml sp services
    private final SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    // execution service for custom attributes mapping
    private ScriptExecutionService executionService;
    private ResourceEntityService resourceService;

    @Autowired
    public SamlIdentityAuthority(
        UserEntityService userEntityService,
        UserAccountService<SamlUserAccount> userAccountService,
        ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository
    ) {
        this(SystemKeys.AUTHORITY_SAML, userEntityService, userAccountService, registrationRepository);
    }

    public SamlIdentityAuthority(
        String authorityId,
        UserEntityService userEntityService,
        UserAccountService<SamlUserAccount> userAccountService,
        ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository
    ) {
        super(authorityId, registrationRepository);
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(userAccountService, "account service is mandatory");

        this.userEntityService = userEntityService;
        this.accountService = userAccountService;

        this.relyingPartyRegistrationRepository = new SamlRelyingPartyRegistrationRepository(registrationRepository);

        // build filter provider
        this.filterProvider = new SamlFilterProvider(
            authorityId,
            relyingPartyRegistrationRepository,
            registrationRepository
        );
    }

    @Autowired
    public void setConfigProvider(SamlIdentityConfigurationProvider configProvider) {
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
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.filterProvider.setApplicationEventPublisher(eventPublisher);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }

    @Override
    public SamlFilterProvider getFilterProvider() {
        return this.filterProvider;
    }

    @Override
    public SamlIdentityProvider buildProvider(SamlIdentityProviderConfig config) {
        String id = config.getProvider();

        SamlIdentityProvider idp = new SamlIdentityProvider(
            authorityId,
            id,
            userEntityService,
            accountService,
            config,
            config.getRealm()
        );

        idp.setExecutionService(executionService);
        idp.setResourceService(resourceService);
        return idp;
    }
}
