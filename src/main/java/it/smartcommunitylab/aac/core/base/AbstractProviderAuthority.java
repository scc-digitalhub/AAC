package it.smartcommunitylab.aac.core.base;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.authorities.ConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.Resource;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfig;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;

public abstract class AbstractProviderAuthority<S extends ConfigurableResourceProvider<R, T, M, C>, R extends Resource, T extends ConfigurableProvider, M extends ConfigMap, C extends ProviderConfig<M, T>>
        implements ConfigurableProviderAuthority<S, R, T, M, C>, InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final String authorityId;

    // provider configs by id
    protected final ProviderConfigRepository<C> registrationRepository;

    // loading cache for idps
    // TODO replace with external loadableProviderRepository for
    // ProviderRepository<InternalIdentityProvider>
    protected final LoadingCache<String, S> providers = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
            .maximumSize(100)
            .build(new CacheLoader<String, S>() {
                @Override
                public S load(final String id) throws Exception {
                    logger.debug("load config from repository for {}", id);
                    C config = registrationRepository.findByProviderId(id);

                    if (config == null) {
                        throw new IllegalArgumentException("no configuration matching the given provider id");
                    }

                    logger.debug("build provider {} config", id);
                    return buildProvider(config);
                }
            });

    public AbstractProviderAuthority(
            String authorityId,
            ProviderConfigRepository<C> registrationRepository) {
        Assert.hasText(authorityId, "authority id  is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository is mandatory");

        this.authorityId = authorityId;
        this.registrationRepository = registrationRepository;
    }

    @Override
    public String getAuthorityId() {
        return authorityId;
    }

    @Override
    public abstract ConfigurationProvider<M, T, C> getConfigurationProvider();

    protected abstract S buildProvider(C config);

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(getConfigurationProvider(), "config provider is mandatory");
    }

    @Override
    public boolean hasProvider(String providerId) {
        C registration = registrationRepository.findByProviderId(providerId);
        return (registration != null);
    }

    @Override
    public C registerProvider(ConfigurableProvider cp) throws RegistrationException {
        // cast config and handle errors
        T tcp = null;
        try {
            @SuppressWarnings("unchecked")
            T t = (T) cp;
            tcp = t;
        } catch (ClassCastException e) {
            logger.error("Wrong config class: " + e.getMessage());
            throw new IllegalArgumentException("unsupported configurable class");
        }
        // we support only matching provider as resource providers
        if (cp != null && getAuthorityId().equals(cp.getAuthority())) {
            String providerId = cp.getProvider();
            String realm = cp.getRealm();

            logger.debug("register provider {} for realm {}", providerId, realm);
            if (logger.isTraceEnabled()) {
                logger.trace("provider config: {}", String.valueOf(cp.getConfiguration()));
            }

            // check if exists or id clashes with another provider from a different realm
            C e = registrationRepository.findByProviderId(providerId);
            if (e != null) {
                if (!realm.equals(e.getRealm())) {
                    // name clash
                    throw new RegistrationException(
                            "a provider with the same id already exists under a different realm");
                }
            }

            try {
                // build config
                C providerConfig = getConfigurationProvider().getConfig(tcp);
                if (logger.isTraceEnabled()) {
                    logger.trace("provider active config: {}",
                            String.valueOf(providerConfig.getConfigMap().getConfiguration()));
                }

                // register, we defer loading
                registrationRepository.addRegistration(providerConfig);

                // load to warm cache
                S rp = providers.get(providerId);

                // return effective config
                return rp.getConfig();
            } catch (Exception ex) {
                // cleanup
                registrationRepository.removeRegistration(providerId);
                logger.error("error registering provider {}: {}", providerId, ex.getMessage());

                throw new RegistrationException("invalid provider configuration: " + ex.getMessage(), ex);
            }
        } else {
            throw new IllegalArgumentException("illegal configurable");
        }
    }

    @Override
    public void unregisterProvider(String providerId) {
        C registration = registrationRepository.findByProviderId(providerId);

        if (registration != null) {
            // can't unregister system providers, check
            if (SystemKeys.REALM_SYSTEM.equals(registration.getRealm())) {
                return;
            }

            logger.debug("unregister provider {} for realm {}", providerId, registration.getRealm());

            // remove from cache
            providers.invalidate(providerId);

            // remove from registrations
            registrationRepository.removeRegistration(providerId);
        }
    }

    @Override
    public S findProvider(String providerId) {
        Assert.hasText(providerId, "provider id can not be null or empty");

        try {
            // check if config is still active
            C config = registrationRepository.findByProviderId(providerId);
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
    public S getProvider(String providerId) throws NoSuchProviderException {
        Assert.hasText(providerId, "provider id can not be null or empty");
        S p = findProvider(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        return p;
    }

    @Override
    public List<S> getProvidersByRealm(String realm) {
        // we need to fetch registrations and get idp from cache, with optional load
        Collection<C> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> findProvider(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }
}
