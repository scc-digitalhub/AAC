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

package it.smartcommunitylab.aac.eidas;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.eidas.model.EIDASUserIdentity;
import it.smartcommunitylab.aac.eidas.provider.EIDASIdentityConfigurationProvider;
import it.smartcommunitylab.aac.eidas.provider.EIDASIdentityProvider;
import it.smartcommunitylab.aac.eidas.provider.EIDASIdentityProviderConfig;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderAuthority;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import it.smartcommunitylab.aac.spid.auth.SpidMetadataRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.spid.auth.SpidRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.spid.provider.SpidFilterProvider;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;

/*
 * eIDAS identity authority.
 *
 * The italian eIDAS node behaves as a SPID identity provider, so the SPID stack is reused: the SPID
 * components are authority-aware (they take the authority from the configuration, the filters from
 * the filter provider), and the eidas module adds the authority, its own provider and configuration,
 * the login button and the eIDAS rules on attributes and levels.
 *
 * Unlike SPID there is no registry of upstream identity providers: a single node is configured per
 * provider via the node metadata url.
 */
@Service
public class EIDASIdentityAuthority
    extends AbstractIdentityProviderAuthority<EIDASIdentityProvider, EIDASUserIdentity, EIDASIdentityProviderConfig, SpidIdentityProviderConfigMap>
    implements ApplicationEventPublisherAware {

    public static final String AUTHORITY_URL = "/auth/" + SystemKeys.AUTHORITY_EIDAS + "/";

    private final UserAccountService<SamlUserAccount> accountService;
    private final SpidFilterProvider filterProvider;

    private ScriptExecutionService executionService;
    private ResourceEntityService resourceService;

    @Autowired
    public EIDASIdentityAuthority(
        UserAccountService<SamlUserAccount> accountService,
        @Qualifier("eidasProviderConfigRepository") ProviderConfigRepository<EIDASIdentityProviderConfig> providerConfigRepository
    ) {
        super(SystemKeys.AUTHORITY_EIDAS, providerConfigRepository);
        this.accountService = accountService;

        // the SPID registration repositories and filters read SpidIdentityProviderConfig; an
        // EIDASIdentityProviderConfig is one, but java generics are invariant, hence the identity
        // translation. The filter provider mounts the SPID filters under /auth/eidas/ and the
        // tokens are routed by the authority of each provider configuration
        TranslatorProviderConfigRepository<EIDASIdentityProviderConfig, SpidIdentityProviderConfig> configRepository =
            new TranslatorProviderConfigRepository<>(providerConfigRepository);
        configRepository.setConverter(config -> config);
        this.filterProvider =
            new SpidFilterProvider(
                SystemKeys.AUTHORITY_EIDAS,
                new SpidRelyingPartyRegistrationRepository(configRepository),
                new SpidMetadataRelyingPartyRegistrationRepository(configRepository),
                configRepository
            );
    }

    @Autowired
    public void setConfigProvider(EIDASIdentityConfigurationProvider configProvider) {
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
    protected EIDASIdentityProvider buildProvider(EIDASIdentityProviderConfig config) {
        String id = config.getProvider();
        EIDASIdentityProvider idp = new EIDASIdentityProvider(id, accountService, config, config.getRealm());
        idp.setExecutionService(executionService);
        idp.setResourceService(resourceService);
        return idp;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.filterProvider.setApplicationEventPublisher(applicationEventPublisher);
    }

    @Override
    public SpidFilterProvider getFilterProvider() {
        return filterProvider;
    }
}
