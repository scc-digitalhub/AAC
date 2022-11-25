package it.smartcommunitylab.aac.password;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractCredentialsAuthority;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.provider.PasswordCredentialsConfigurationProvider;
import it.smartcommunitylab.aac.password.provider.PasswordCredentialsService;
import it.smartcommunitylab.aac.password.provider.PasswordCredentialsServiceConfig;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfigMap;
import it.smartcommunitylab.aac.password.service.InternalPasswordUserCredentialsService;
import it.smartcommunitylab.aac.utils.MailService;

/*
 * Password service depends on password identity provider
 * 
 * every idp will expose a matching service with the same configuration for credentials handling
 */
@Service
public class PasswordCredentialsAuthority extends
        AbstractCredentialsAuthority<PasswordCredentialsService, InternalUserPassword, PasswordIdentityProviderConfigMap, PasswordCredentialsServiceConfig> {

    public static final String AUTHORITY_URL = "/auth/password/";

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;

    // password service
    private final InternalPasswordUserCredentialsService passwordService;

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;
    private ResourceEntityService resourceService;

    public PasswordCredentialsAuthority(
            UserAccountService<InternalUserAccount> userAccountService,
            InternalPasswordUserCredentialsService passwordService,
            ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_PASSWORD, new PasswordConfigTranslatorRepository(registrationRepository));
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(passwordService, "password service is mandatory");

        this.accountService = userAccountService;
        this.passwordService = passwordService;
    }

    @Autowired
    public void setConfigProvider(PasswordCredentialsConfigurationProvider configProvider) {
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

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public PasswordCredentialsService buildProvider(PasswordCredentialsServiceConfig config) {
        PasswordCredentialsService service = new PasswordCredentialsService(
                config.getProvider(),
                accountService, passwordService,
                config, config.getRealm());

        service.setMailService(mailService);
        service.setUriBuilder(uriBuilder);
        service.setResourceService(resourceService);

        return service;
    }

    @Override
    public PasswordCredentialsServiceConfig registerProvider(ConfigurableProvider cp) {
        throw new IllegalArgumentException("direct registration not supported");
    }

    static class PasswordConfigTranslatorRepository extends
            TranslatorProviderConfigRepository<PasswordIdentityProviderConfig, PasswordCredentialsServiceConfig> {

        public PasswordConfigTranslatorRepository(
                ProviderConfigRepository<PasswordIdentityProviderConfig> externalRepository) {
            super(externalRepository);
            setConverter((source) -> {
                PasswordCredentialsServiceConfig config = new PasswordCredentialsServiceConfig(source.getProvider(),
                        source.getRealm());
                config.setName(source.getName());
                config.setTitleMap(source.getTitleMap());
                config.setDescriptionMap(source.getDescriptionMap());

                // we share the same configMap
                config.setConfigMap(source.getConfigMap());
                config.setRepositoryId(source.getRepositoryId());

                return config;
            });
        }

    }
}
