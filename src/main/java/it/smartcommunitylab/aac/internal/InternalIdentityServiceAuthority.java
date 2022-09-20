package it.smartcommunitylab.aac.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.IdentityServiceAuthority;
import it.smartcommunitylab.aac.core.base.AbstractAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityServiceConfigMap;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityServiceConfigurationProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityService;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityServiceConfig;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;

@Service
public class InternalIdentityServiceAuthority
        extends
        AbstractAuthority<InternalIdentityService, InternalUserIdentity, ConfigurableIdentityService, InternalIdentityServiceConfigMap, InternalIdentityServiceConfig>
        implements
        IdentityServiceAuthority<InternalIdentityService, InternalUserIdentity, InternalUserAccount, InternalIdentityServiceConfigMap, InternalIdentityServiceConfig> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String AUTHORITY_URL = "/auth/internal/";

    // user service
    private final UserEntityService userEntityService;

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;
    private final InternalUserConfirmKeyService confirmKeyService;

    // configuration provider
    protected InternalIdentityServiceConfigurationProvider configProvider;

    public InternalIdentityServiceAuthority(
            UserEntityService userEntityService,
            UserAccountService<InternalUserAccount> userAccountService, InternalUserConfirmKeyService confirmKeyService,
            ProviderConfigRepository<InternalIdentityServiceConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_INTERNAL, registrationRepository);
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(confirmKeyService, "confirm key service is mandatory");

        this.userEntityService = userEntityService;
        this.accountService = userAccountService;
        this.confirmKeyService = confirmKeyService;
    }

    @Autowired
    public void setConfigProvider(InternalIdentityServiceConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Override
    public InternalIdentityServiceConfigurationProvider getConfigurationProvider() {
        return configProvider;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    protected InternalIdentityService buildProvider(InternalIdentityServiceConfig config) {
        InternalIdentityService idp = new InternalIdentityService(
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
