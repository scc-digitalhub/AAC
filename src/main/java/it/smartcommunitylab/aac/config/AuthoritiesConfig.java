package it.smartcommunitylab.aac.config;

import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.authorities.IdentityProviderAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.AccountServiceAuthorityService;
import it.smartcommunitylab.aac.core.service.IdentityProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.InMemoryProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.openid.OIDCAccountServiceAuthority;
import it.smartcommunitylab.aac.openid.OIDCIdentityAuthority;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityConfigurationProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;

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
            IdentityAuthoritiesProperties authsProps) {

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
                        OIDCIdentityConfigurationProvider configProvider = new OIDCIdentityConfigurationProvider(id,
                                configMap);

                        // build config repositories
                        ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository = new InMemoryProviderConfigRepository<>();
                        // instantiate authority
                        OIDCIdentityAuthority auth = new OIDCIdentityAuthority(
                                id,
                                oidcUserAccountService,
                                registrationRepository);

                        auth.setConfigProvider(configProvider);
                        auth.setExecutionService(executionService);
                        auth.setResourceService(resourceService);

                        // register for manager
                        service.registerAuthority(auth);

                        // also register connected account service
                        OIDCAccountServiceAuthority aauth = new OIDCAccountServiceAuthority(
                                id, oidcUserAccountService, registrationRepository);
                        aauth.setResourceService(resourceService);
                        accountServiceAuthorityService.registerAuthority(aauth);

                    }
                }
            }

        }

        return service;

    }

}
