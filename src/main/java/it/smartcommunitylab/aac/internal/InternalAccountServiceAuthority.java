package it.smartcommunitylab.aac.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.AccountServiceAuthority;
import it.smartcommunitylab.aac.core.base.AbstractSingleProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountService;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalAccountServiceConfigMap;
import it.smartcommunitylab.aac.internal.provider.InternalAccountServiceConfigurationProvider;
import it.smartcommunitylab.aac.internal.provider.InternalAccountService;
import it.smartcommunitylab.aac.internal.provider.InternalAccountServiceConfig;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;

@Service
public class InternalAccountServiceAuthority
        extends
        AbstractSingleProviderAuthority<InternalAccountService, InternalUserAccount, ConfigurableAccountService, InternalAccountServiceConfigMap, InternalAccountServiceConfig>
        implements
        AccountServiceAuthority<InternalAccountService, InternalUserAccount, InternalAccountServiceConfigMap, InternalAccountServiceConfig> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String AUTHORITY_URL = "/auth/internal/";

    // user service
    private final UserEntityService userEntityService;

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;
    private final InternalUserConfirmKeyService confirmKeyService;

    // configuration provider
    protected InternalAccountServiceConfigurationProvider configProvider;

    public InternalAccountServiceAuthority(
            UserEntityService userEntityService,
            UserAccountService<InternalUserAccount> userAccountService, InternalUserConfirmKeyService confirmKeyService,
            ProviderConfigRepository<InternalAccountServiceConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_INTERNAL, registrationRepository);
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(confirmKeyService, "confirm key service is mandatory");

        this.userEntityService = userEntityService;
        this.accountService = userAccountService;
        this.confirmKeyService = confirmKeyService;
    }

    @Autowired
    public void setConfigProvider(InternalAccountServiceConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
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
        InternalAccountService idp = new InternalAccountService(
                config.getProvider(),
                userEntityService,
                accountService, confirmKeyService,
                config, config.getRealm());

        return idp;
    }

    @Override
    public FilterProvider getFilterProvider() {
        // TODO Auto-generated method stub
        return null;
    }

}
