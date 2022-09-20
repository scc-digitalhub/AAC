package it.smartcommunitylab.aac.internal;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.authorities.IdentityProviderAuthority;
import it.smartcommunitylab.aac.core.authorities.ProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityFilterProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityServiceConfigMap;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigurationProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityServiceConfig;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;

@Service
public class InternalIdentityProviderAuthority
        implements
        ProviderAuthority<InternalIdentityProvider, InternalUserIdentity, ConfigurableIdentityProvider, InternalIdentityServiceConfigMap, InternalIdentityProviderConfig>,
        IdentityProviderAuthority<InternalIdentityProvider, InternalUserIdentity, InternalIdentityServiceConfigMap, InternalIdentityProviderConfig>,
        InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String AUTHORITY_URL = "/auth/internal/";

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;
    private final InternalUserConfirmKeyService confirmKeyService;

    // filter provider
    private final InternalIdentityFilterProvider filterProvider;

    // configuration provider
    protected InternalIdentityProviderConfigurationProvider configProvider;

    // provider configs by id
    protected final ProviderConfigRepository<InternalIdentityServiceConfig> registrationRepository;

    // loading cache for idps
    // TODO replace with external loadableProviderRepository for
    // ProviderRepository<InternalIdentityProvider>
    private final LoadingCache<String, InternalIdentityProvider> providers = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
            .maximumSize(100)
            .build(new CacheLoader<String, InternalIdentityProvider>() {
                @Override
                public InternalIdentityProvider load(final String id) throws Exception {
                    logger.debug("load config from repository for {}", id);
                    InternalIdentityServiceConfig config = registrationRepository.findByProviderId(id);

                    if (config == null) {
                        throw new IllegalArgumentException("no configuration matching the given provider id");
                    }

                    logger.debug("build provider {} config", id);
                    InternalIdentityProviderConfig idpConfig = new InternalIdentityProviderConfig(config);
                    return buildProvider(idpConfig);
                }
            });

    public InternalIdentityProviderAuthority(
            UserAccountService<InternalUserAccount> userAccountService, InternalUserConfirmKeyService confirmKeyService,
            ProviderConfigRepository<InternalIdentityServiceConfig> registrationRepository) {
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(confirmKeyService, "confirm key service is mandatory");
        Assert.notNull(registrationRepository, "config repository is mandatory");

        this.accountService = userAccountService;
        this.confirmKeyService = confirmKeyService;

        this.registrationRepository = registrationRepository;

        // build filter provider
        this.filterProvider = new InternalIdentityFilterProvider(userAccountService, confirmKeyService,
                registrationRepository);
    }

    @Autowired
    public void setConfigProvider(InternalIdentityProviderConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    public InternalIdentityProviderConfigurationProvider getConfigurationProvider() {
        return configProvider;
    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_INTERNAL;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    private InternalIdentityProvider buildProvider(InternalIdentityProviderConfig config) {
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

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(getConfigurationProvider(), "config provider is mandatory");
    }

    @Override
    public boolean hasProvider(String providerId) {
        InternalIdentityServiceConfig registration = registrationRepository.findByProviderId(providerId);
        return (registration != null);
    }

    @Override
    public InternalIdentityProvider findProvider(String providerId) {
        Assert.hasText(providerId, "provider id can not be null or empty");

        try {
            // check if config is still active
            InternalIdentityServiceConfig config = registrationRepository.findByProviderId(providerId);
            if (config == null) {
                // cleanup cache
                providers.invalidate(providerId);

                return null;
            }

            return providers.get(providerId);
        } catch (IllegalArgumentException | UncheckedExecutionException | ExecutionException e) {
            return null;
        }
    }

    @Override
    public InternalIdentityProvider getProvider(String providerId) throws NoSuchProviderException {
        Assert.hasText(providerId, "provider id can not be null or empty");
        InternalIdentityProvider p = findProvider(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        return p;
    }

    @Override
    public List<InternalIdentityProvider> getProvidersByRealm(String realm) {
        // we need to fetch registrations and get idp from cache, with optional load
        Collection<InternalIdentityServiceConfig> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> findProvider(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }

    @Override
    public InternalIdentityProvider registerProvider(ConfigurableIdentityProvider config)
            throws IllegalArgumentException, RegistrationException, SystemException {
        // nothing to do
        throw new RegistrationException();
    }

    @Override
    public void unregisterProvider(String providerId) throws SystemException {
        // nothing to do

    }

}
