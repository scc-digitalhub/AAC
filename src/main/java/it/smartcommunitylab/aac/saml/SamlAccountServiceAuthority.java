package it.smartcommunitylab.aac.saml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.AccountServiceAuthority;
import it.smartcommunitylab.aac.core.base.AbstractEditableAccount;
import it.smartcommunitylab.aac.core.base.AbstractProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.saml.provider.SamlAccountService;
import it.smartcommunitylab.aac.saml.provider.SamlAccountServiceConfig;
import it.smartcommunitylab.aac.saml.provider.SamlAccountServiceConfigConverter;
import it.smartcommunitylab.aac.saml.provider.SamlAccountServiceConfigurationProvider;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfigMap;

@Service
public class SamlAccountServiceAuthority
        extends
        AbstractProviderAuthority<SamlAccountService, SamlUserAccount, ConfigurableAccountProvider, SamlIdentityProviderConfigMap, SamlAccountServiceConfig>
        implements
        AccountServiceAuthority<SamlAccountService, SamlUserAccount, AbstractEditableAccount, SamlIdentityProviderConfigMap, SamlAccountServiceConfig> {

    // account service
    private final UserAccountService<SamlUserAccount> accountService;
    private ResourceEntityService resourceService;

    // configuration provider
    protected SamlAccountServiceConfigurationProvider configProvider;

    @Autowired
    public SamlAccountServiceAuthority(
            UserAccountService<SamlUserAccount> userAccountService,
            ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository) {
        this(SystemKeys.AUTHORITY_SAML, userAccountService, registrationRepository);
    }

    public SamlAccountServiceAuthority(
            String authority,
            UserAccountService<SamlUserAccount> userAccountService,
            ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository) {
        super(authority, new SamlConfigTranslatorRepository(registrationRepository));
        Assert.notNull(userAccountService, "account service is mandatory");

        this.accountService = userAccountService;
        this.configProvider = new SamlAccountServiceConfigurationProvider(authority);
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public SamlAccountServiceConfigurationProvider getConfigurationProvider() {
        return configProvider;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    protected SamlAccountService buildProvider(SamlAccountServiceConfig config) {
        SamlAccountService service = new SamlAccountService(
                config.getProvider(),
                accountService,
                config, config.getRealm());
        service.setResourceService(resourceService);

        return service;
    }

    @Override
    public SamlAccountServiceConfig registerProvider(ConfigurableProvider cp) {
        throw new IllegalArgumentException("direct registration not supported");
    }

    static class SamlConfigTranslatorRepository extends
            TranslatorProviderConfigRepository<SamlIdentityProviderConfig, SamlAccountServiceConfig> {

        public SamlConfigTranslatorRepository(
                ProviderConfigRepository<SamlIdentityProviderConfig> externalRepository) {
            super(externalRepository);
            setConverter(new SamlAccountServiceConfigConverter());
        }

    }
}
