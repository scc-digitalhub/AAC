package it.smartcommunitylab.aac.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.AccountServiceAuthority;
import it.smartcommunitylab.aac.core.base.AbstractSingleProviderAuthority;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.model.InternalEditableUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalAccountServiceConfigurationProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;
import it.smartcommunitylab.aac.internal.provider.InternalAccountService;
import it.smartcommunitylab.aac.internal.provider.InternalAccountServiceConfig;
import it.smartcommunitylab.aac.internal.provider.InternalAccountServiceConfigConverter;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;
import it.smartcommunitylab.aac.utils.MailService;

@Service
public class InternalAccountServiceAuthority
        extends
        AbstractSingleProviderAuthority<InternalAccountService, InternalUserAccount, ConfigurableAccountProvider, InternalIdentityProviderConfigMap, InternalAccountServiceConfig>
        implements
        AccountServiceAuthority<InternalAccountService, InternalUserAccount, InternalEditableUserAccount, InternalIdentityProviderConfigMap, InternalAccountServiceConfig> {

    public static final String AUTHORITY_URL = "/auth/internal/";

    // user service
    private final UserEntityService userEntityService;
    private final ResourceEntityService resourceService;

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;
    private final InternalUserConfirmKeyService confirmKeyService;

    // configuration provider
    protected InternalAccountServiceConfigurationProvider configProvider;

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    public InternalAccountServiceAuthority(
            UserEntityService userEntityService, ResourceEntityService resourceService,
            UserAccountService<InternalUserAccount> userAccountService, InternalUserConfirmKeyService confirmKeyService,
            ProviderConfigRepository<InternalIdentityProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_INTERNAL, new InternalConfigTranslatorRepository(registrationRepository));
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(resourceService, "resource service is mandatory");
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(confirmKeyService, "confirm key service is mandatory");

        this.userEntityService = userEntityService;
        this.resourceService = resourceService;
        this.accountService = userAccountService;
        this.confirmKeyService = confirmKeyService;
    }

    @Autowired
    public void setConfigProvider(InternalAccountServiceConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Autowired
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    @Autowired
    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    @Override
    public InternalAccountServiceConfigurationProvider getConfigurationProvider() {
        return configProvider;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    protected InternalAccountService buildProvider(InternalAccountServiceConfig config) {
        InternalAccountService service = new InternalAccountService(
                config.getProvider(),
                userEntityService,
                accountService, confirmKeyService,
                config, config.getRealm());

        service.setMailService(mailService);
        service.setUriBuilder(uriBuilder);
        service.setResourceService(resourceService);

        return service;
    }

    @Override
    public InternalAccountServiceConfig registerProvider(ConfigurableProvider cp) {
        throw new IllegalArgumentException("direct registration not supported");
    }

    static class InternalConfigTranslatorRepository extends
            TranslatorProviderConfigRepository<InternalIdentityProviderConfig, InternalAccountServiceConfig> {

        public InternalConfigTranslatorRepository(
                ProviderConfigRepository<InternalIdentityProviderConfig> externalRepository) {
            super(externalRepository);
            setConverter(new InternalAccountServiceConfigConverter());
        }

    }
}
