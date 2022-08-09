package it.smartcommunitylab.aac.config;

import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.authorities.IdentityProviderAuthority;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.IdentityProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.InMemoryProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.openid.OIDCIdentityAuthority;
import it.smartcommunitylab.aac.openid.auth.OIDCClientRegistrationRepository;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityConfigurationProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openid.service.OIDCUserAccountService;
import it.smartcommunitylab.aac.saml.service.SamlUserAccountService;

/*
 * Authorities configuration
 */
@Configuration
@Order(12)
public class AuthoritiesConfig {

    @Autowired
    private UserEntityService userEntityService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private InternalUserAccountService internalUserAccountService;

    @Autowired
    private OIDCUserAccountService oidcUserAccountService;

    @Autowired
    private SamlUserAccountService samlUserAccountService;

    @Autowired
    private AutoJdbcAttributeStore jdbcAttributeStore;

    @Autowired
    private ScriptExecutionService executionService;

    @Bean
    public IdentityProviderAuthorityService identityProviderAuthorityService(
            Collection<? extends IdentityProviderAuthority<? extends UserIdentity, ? extends IdentityProvider<? extends UserIdentity>>> authorities,
            AuthoritiesProperties authsProps) {

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
                        // buid oidc config provider
                        OIDCIdentityProviderConfigMap configMap = authProp.getOidc();
                        OIDCIdentityConfigurationProvider configProvider = new OIDCIdentityConfigurationProvider(id,
                                configMap);

                        // build config repositories
                        ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository = new InMemoryProviderConfigRepository<>();
                        OIDCClientRegistrationRepository clientRegistrationRepository = new OIDCClientRegistrationRepository();
                        // instantiate authority
                        OIDCIdentityAuthority auth = new OIDCIdentityAuthority(
                                id,
                                userEntityService, subjectService,
                                oidcUserAccountService, jdbcAttributeStore,
                                registrationRepository,
                                clientRegistrationRepository);

                        auth.setConfigProvider(configProvider);
                        auth.setExecutionService(executionService);

                        // register for manager
                        service.registerAuthority(auth);
                    }
                }
            }

        }

        return service;

    }

}
