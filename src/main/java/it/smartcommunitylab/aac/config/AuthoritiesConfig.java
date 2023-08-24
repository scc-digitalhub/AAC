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

package it.smartcommunitylab.aac.config;

import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.accounts.service.AccountServiceAuthorityService;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.InMemoryProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.identity.IdentityProviderAuthority;
import it.smartcommunitylab.aac.identity.service.IdentityProviderAuthorityService;
import it.smartcommunitylab.aac.oidc.OIDCAccountServiceAuthority;
import it.smartcommunitylab.aac.oidc.OIDCIdentityAuthority;
import it.smartcommunitylab.aac.oidc.model.OIDCUserAccount;
import it.smartcommunitylab.aac.oidc.provider.OIDCIdentityConfigurationProvider;
import it.smartcommunitylab.aac.oidc.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.oidc.provider.OIDCIdentityProviderConfigMap;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

/*
 * Authorities configuration
 */
@Configuration
@Order(12)
public class AuthoritiesConfig {

    @Autowired
    private UserAccountService<OIDCUserAccount> oidcUserAccountService;

    @Autowired
    private ScriptExecutionService executionService;

    @Autowired
    private ResourceEntityService resourceService;

    @Autowired
    private AccountServiceAuthorityService accountServiceAuthorityService;

    @Bean
    public IdentityProviderAuthorityService identityProviderAuthorityService(
        Collection<IdentityProviderAuthority<?, ?, ?, ?>> authorities,
        IdentityAuthoritiesProperties authsProps
    ) {
        // build a service with default from autowiring
        IdentityProviderAuthorityService service = new IdentityProviderAuthorityService(authorities);

        // load custom authorities and build
        if (authsProps.getCustom() != null) {
            for (CustomAuthoritiesProperties authProp : authsProps.getCustom()) {
                // read props
                String id = authProp.getId();
                String name = authProp.getName();
                String description = authProp.getDescription();

                if (StringUtils.hasText(id)) {
                    // derive type manually
                    // TODO refactor

                    if (authProp.getOidc() != null) {
                        // build oidc config provider
                        OIDCIdentityProviderConfigMap configMap = authProp.getOidc();
                        OIDCIdentityConfigurationProvider configProvider = new OIDCIdentityConfigurationProvider(
                            id,
                            configMap
                        );

                        // build config repositories
                        ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository =
                            new InMemoryProviderConfigRepository<>();
                        // instantiate authority
                        OIDCIdentityAuthority auth = new OIDCIdentityAuthority(
                            id,
                            oidcUserAccountService,
                            registrationRepository
                        );

                        auth.setConfigProvider(configProvider);
                        auth.setExecutionService(executionService);
                        auth.setResourceService(resourceService);

                        // register for manager
                        service.registerAuthority(auth);

                        // also register connected account service
                        OIDCAccountServiceAuthority aauth = new OIDCAccountServiceAuthority(
                            id,
                            oidcUserAccountService,
                            registrationRepository
                        );
                        aauth.setResourceService(resourceService);
                        accountServiceAuthorityService.registerAuthority(aauth);
                    }
                }
            }
        }

        return service;
    }
}
