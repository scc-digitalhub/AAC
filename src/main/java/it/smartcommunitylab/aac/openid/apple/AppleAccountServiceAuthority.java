package it.smartcommunitylab.aac.openid.apple;

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
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.openid.apple.provider.AppleAccountService;
import it.smartcommunitylab.aac.openid.apple.provider.AppleAccountServiceConfig;
import it.smartcommunitylab.aac.openid.apple.provider.AppleAccountServiceConfigurationProvider;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;

@Service
public class AppleAccountServiceAuthority
        extends
        AbstractProviderAuthority<AppleAccountService, OIDCUserAccount, ConfigurableAccountProvider, AppleIdentityProviderConfigMap, AppleAccountServiceConfig>
        implements
        AccountServiceAuthority<AppleAccountService, OIDCUserAccount, AbstractEditableAccount, AppleIdentityProviderConfigMap, AppleAccountServiceConfig> {

    public static final String AUTHORITY_URL = "/auth/apple/";

    // account service
    private final UserAccountService<OIDCUserAccount> accountService;

    // configuration provider
    protected AppleAccountServiceConfigurationProvider configProvider;

    public AppleAccountServiceAuthority(
            UserAccountService<OIDCUserAccount> userAccountService,
            ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_APPLE, new AppleConfigTranslatorRepository(registrationRepository));
        Assert.notNull(userAccountService, "account service is mandatory");

        this.accountService = userAccountService;
        this.configProvider = new AppleAccountServiceConfigurationProvider();
    }

    @Override
    public AppleAccountServiceConfigurationProvider getConfigurationProvider() {
        return configProvider;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    protected AppleAccountService buildProvider(AppleAccountServiceConfig config) {
        AppleAccountService idp = new AppleAccountService(
                config.getProvider(),
                accountService,
                config, config.getRealm());

        return idp;
    }

    @Override
    public AppleAccountServiceConfig registerProvider(ConfigurableProvider cp) {
        throw new IllegalArgumentException("direct registration not supported");
    }

    static class AppleConfigTranslatorRepository extends
            TranslatorProviderConfigRepository<AppleIdentityProviderConfig, AppleAccountServiceConfig> {

        public AppleConfigTranslatorRepository(
                ProviderConfigRepository<AppleIdentityProviderConfig> externalRepository) {
            super(externalRepository);
            setConverter((source) -> {
                AppleAccountServiceConfig config = new AppleAccountServiceConfig(
                        source.getProvider(),
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
