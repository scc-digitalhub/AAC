package it.smartcommunitylab.aac.spid;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.config.SpidProperties;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderAuthority;
import it.smartcommunitylab.aac.spid.auth.SpidRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.spid.model.SpidUserIdentity;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import it.smartcommunitylab.aac.spid.provider.*;
import it.smartcommunitylab.aac.spid.service.LocalSpidRegistry;
import it.smartcommunitylab.aac.spid.service.SpidRegistry;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;

@Service
public class SpidIdentityAuthority
    extends AbstractIdentityProviderAuthority<SpidIdentityProvider, SpidUserIdentity, SpidIdentityProviderConfig, SpidIdentityProviderConfigMap>
    implements ApplicationEventPublisherAware {

    public static final String AUTHORITY_URL = "/auth/" + SystemKeys.AUTHORITY_SPID + "/";
    private final UserAccountService<SpidUserAccount> accountService;
    private final SpidRelyingPartyRegistrationRepository registrationRepository;
    private final ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository;
    private final SpidFilterProvider filterProvider;
    private ScriptExecutionService executionService;
    private ResourceEntityService resourceService;

    private final LoadingCache<String, SpidIdentityProvider> providers = CacheBuilder
        .newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(100)
        .build(
            new CacheLoader<String, SpidIdentityProvider>() {
                @Override
                public SpidIdentityProvider load(String id) throws Exception {
                    SpidIdentityProviderConfig config = providerConfigRepository.findByProviderId(id);
                    if (config == null) {
                        throw new IllegalArgumentException("no configuration matches id " + id);
                    }
                    config.setIdentityProviders(spidRegistry.getIdentityProviders());
                    return new SpidIdentityProvider(id, accountService, config, config.getRealm());
                }
            }
        );

    // configuration
    private SpidRegistry spidRegistry; // registry with upstream idp
    private SpidProperties spidProperties;

    public SpidIdentityAuthority(
        String authorityId,
        UserAccountService<SpidUserAccount> accountService,
        ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository
    ) {
        super(authorityId, providerConfigRepository);
        this.accountService = accountService;
        this.providerConfigRepository = providerConfigRepository;
        this.registrationRepository = new SpidRelyingPartyRegistrationRepository(providerConfigRepository);
        this.filterProvider = new SpidFilterProvider(authorityId, registrationRepository, providerConfigRepository);
    }

    @Autowired
    public SpidIdentityAuthority(
        UserAccountService<SpidUserAccount> accountService,
        ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository
    ) {
        this(SystemKeys.AUTHORITY_SPID, accountService, providerConfigRepository);
    }

    @Autowired
    public void setConfigProvider(SpidIdentityConfigurationProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Autowired
    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        if (spidProperties != null) {
            // TODO: fetch from online registry
            spidRegistry = new LocalSpidRegistry(spidProperties);
        }
    }

    @Override
    protected SpidIdentityProvider buildProvider(SpidIdentityProviderConfig config) {
        String id = config.getProvider();
        SpidIdentityProvider idp = new SpidIdentityProvider(authorityId, id, accountService, config, config.getRealm());
        idp.setExecutionService(executionService);
        idp.setResourceService(resourceService);
        return idp;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.filterProvider.setApplicationEventPublisher(applicationEventPublisher);
    }
}
