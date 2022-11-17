package it.smartcommunitylab.aac.openid;

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
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.provider.OIDCAccountService;
import it.smartcommunitylab.aac.openid.provider.OIDCAccountServiceConfig;
import it.smartcommunitylab.aac.openid.provider.OIDCAccountServiceConfigurationProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;

@Service
public class OIDCAccountServiceAuthority
        extends
        AbstractProviderAuthority<OIDCAccountService, OIDCUserAccount, ConfigurableAccountProvider, OIDCIdentityProviderConfigMap, OIDCAccountServiceConfig>
        implements
        AccountServiceAuthority<OIDCAccountService, OIDCUserAccount, AbstractEditableAccount, OIDCIdentityProviderConfigMap, OIDCAccountServiceConfig> {

    public static final String AUTHORITY_URL = "/auth/oidc/";

    // account service
    private final UserAccountService<OIDCUserAccount> accountService;

    // configuration provider
    protected OIDCAccountServiceConfigurationProvider configProvider;

    @Autowired
    public OIDCAccountServiceAuthority(
            UserAccountService<OIDCUserAccount> userAccountService,
            ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository) {
        this(SystemKeys.AUTHORITY_OIDC, userAccountService, registrationRepository);
    }

    public OIDCAccountServiceAuthority(
            String authority,
            UserAccountService<OIDCUserAccount> userAccountService,
            ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository) {
        super(authority, new OIDCConfigTranslatorRepository(registrationRepository));
        Assert.notNull(userAccountService, "account service is mandatory");

        this.accountService = userAccountService;
        this.configProvider = new OIDCAccountServiceConfigurationProvider(authority);
    }

    @Override
    public OIDCAccountServiceConfigurationProvider getConfigurationProvider() {
        return configProvider;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    protected OIDCAccountService buildProvider(OIDCAccountServiceConfig config) {
        OIDCAccountService idp = new OIDCAccountService(
                config.getProvider(),
                accountService,
                config, config.getRealm());

        return idp;
    }

    @Override
    public OIDCAccountServiceConfig registerProvider(ConfigurableProvider cp) {
        throw new IllegalArgumentException("direct registration not supported");
    }

    static class OIDCConfigTranslatorRepository extends
            TranslatorProviderConfigRepository<OIDCIdentityProviderConfig, OIDCAccountServiceConfig> {

        public OIDCConfigTranslatorRepository(
                ProviderConfigRepository<OIDCIdentityProviderConfig> externalRepository) {
            super(externalRepository);
            setConverter((source) -> {
                OIDCAccountServiceConfig config = new OIDCAccountServiceConfig(source.getAuthority(),
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
