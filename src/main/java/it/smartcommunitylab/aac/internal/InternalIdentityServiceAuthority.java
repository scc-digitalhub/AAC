package it.smartcommunitylab.aac.internal;

import java.util.Collection;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.CredentialsServiceAuthority;
import it.smartcommunitylab.aac.core.authorities.IdentityServiceAuthority;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.repository.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.model.InternalEditableUserAccount;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityService;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityServiceConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;

@Service
public class InternalIdentityServiceAuthority
        extends
        AbstractConfigurableProviderAuthority<InternalIdentityService, InternalUserIdentity, ConfigurableIdentityService, InternalIdentityProviderConfigMap, InternalIdentityServiceConfig>
        implements
        IdentityServiceAuthority<InternalIdentityService, InternalUserIdentity, InternalUserAccount, InternalEditableUserAccount, InternalIdentityProviderConfigMap, InternalIdentityServiceConfig> {

    public static final String AUTHORITY_URL = "/auth/internal/";

    // internal authorities
    private final InternalAccountServiceAuthority accountServiceAuthority;
    private final Collection<CredentialsServiceAuthority<?, ?, ?, ?, ?>> credentialsServiceAuthorities;

    // services
    private final UserEntityService userEntityService;

    public InternalIdentityServiceAuthority(
            UserEntityService userEntityService,
            InternalAccountServiceAuthority accountServiceAuthority,
            Collection<CredentialsServiceAuthority<?, ?, ?, ?, ?>> credentialsServiceAuthorities,
            ProviderConfigRepository<InternalIdentityProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_INTERNAL, new InternalConfigTranslatorRepository(registrationRepository));
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(accountServiceAuthority, "account service authority is mandatory");

        this.accountServiceAuthority = accountServiceAuthority;
        this.credentialsServiceAuthorities = credentialsServiceAuthorities;

        this.userEntityService = userEntityService;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    protected InternalIdentityService buildProvider(InternalIdentityServiceConfig config) {
        InternalIdentityService idp = new InternalIdentityService(
                config.getProvider(), userEntityService,
                config, config.getRealm());

        idp.setAccountServiceAuthority(accountServiceAuthority);
        idp.setCredentialsServiceAuthorities(credentialsServiceAuthorities);
        return idp;
    }

    @Override
    public FilterProvider getFilterProvider() {
        // TODO add filters for registration and for credentials management
        return null;
    }

    static class InternalConfigTranslatorRepository extends
            TranslatorProviderConfigRepository<InternalIdentityProviderConfig, InternalIdentityServiceConfig> {

        public InternalConfigTranslatorRepository(
                ProviderConfigRepository<InternalIdentityProviderConfig> externalRepository) {
            super(externalRepository);
            setConverter((source) -> {
                InternalIdentityServiceConfig config = new InternalIdentityServiceConfig(source.getProvider(),
                        source.getRealm());
                config.setName(source.getName());
                config.setTitleMap(source.getTitleMap());
                config.setDescriptionMap(source.getDescriptionMap());

                // we share the same configMap
                config.setConfigMap(source.getConfigMap());
                return config;

            });
        }

    }

}
