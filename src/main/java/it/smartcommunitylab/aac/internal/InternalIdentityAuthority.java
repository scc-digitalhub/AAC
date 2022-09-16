package it.smartcommunitylab.aac.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityFilterProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityConfigurationProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;

@Service
public class InternalIdentityAuthority
        extends
        AbstractIdentityAuthority<InternalIdentityProvider, InternalUserIdentity, InternalIdentityProviderConfigMap, InternalIdentityProviderConfig> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String AUTHORITY_URL = "/auth/internal/";

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;
    private final InternalUserConfirmKeyService confirmKeyService;

    // filter provider
    private final InternalIdentityFilterProvider filterProvider;

    public InternalIdentityAuthority(
            UserAccountService<InternalUserAccount> userAccountService, InternalUserConfirmKeyService confirmKeyService,
            ProviderConfigRepository<InternalIdentityProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_INTERNAL, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(confirmKeyService, "confirm key service is mandatory");

        this.accountService = userAccountService;
        this.confirmKeyService = confirmKeyService;

        // build filter provider
        this.filterProvider = new InternalIdentityFilterProvider(userAccountService, confirmKeyService,
                registrationRepository);
    }

    @Autowired
    public void setConfigProvider(InternalIdentityConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Override
    public InternalIdentityProvider buildProvider(InternalIdentityProviderConfig config) {
        InternalIdentityProvider idp = new InternalIdentityProvider(
                config.getProvider(),
                accountService, confirmKeyService,
                config, config.getRealm());

        return idp;
    }

    @Override
    public InternalIdentityFilterProvider getFilterProvider() {
        return filterProvider;
    }

}
