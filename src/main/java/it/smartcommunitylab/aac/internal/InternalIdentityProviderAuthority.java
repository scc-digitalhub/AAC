package it.smartcommunitylab.aac.internal;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractSingleProviderIdentityAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityFilterProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigurationProvider;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class InternalIdentityProviderAuthority
    extends AbstractSingleProviderIdentityAuthority<InternalIdentityProvider, InternalUserIdentity, InternalIdentityProviderConfigMap, InternalIdentityProviderConfig>
    implements InitializingBean {

    public static final String AUTHORITY_URL = "/auth/internal/";

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;
    private final InternalUserConfirmKeyService confirmKeyService;

    // filter provider
    private final InternalIdentityFilterProvider filterProvider;

    //resource service for accounts
    private ResourceEntityService resourceService;

    public InternalIdentityProviderAuthority(
        UserAccountService<InternalUserAccount> userAccountService,
        InternalUserConfirmKeyService confirmKeyService,
        ProviderConfigRepository<InternalIdentityProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_INTERNAL, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(confirmKeyService, "confirm key service is mandatory");
        Assert.notNull(registrationRepository, "config repository is mandatory");

        this.accountService = userAccountService;
        this.confirmKeyService = confirmKeyService;

        // build filter provider
        this.filterProvider =
            new InternalIdentityFilterProvider(userAccountService, confirmKeyService, registrationRepository);
    }

    @Autowired
    public void setConfigProvider(InternalIdentityProviderConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    protected InternalIdentityProvider buildProvider(InternalIdentityProviderConfig config) {
        InternalIdentityProvider idp = new InternalIdentityProvider(
            config.getProvider(),
            accountService,
            confirmKeyService,
            config,
            config.getRealm()
        );

        idp.setResourceService(resourceService);
        return idp;
    }

    @Override
    public InternalIdentityFilterProvider getFilterProvider() {
        return filterProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Assert.notNull(getConfigurationProvider(), "config provider is mandatory");
    }
}
