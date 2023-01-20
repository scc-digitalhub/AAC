package it.smartcommunitylab.aac.core.base;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.authorities.ProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.Resource;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;

public abstract class AbstractConfigurableProviderAuthority<S extends ConfigurableResourceProvider<R, T, M, C>, R extends Resource, T extends ConfigurableProvider, M extends AbstractConfigMap, C extends AbstractProviderConfig<M, T>>
        implements ProviderAuthority<S, R> {
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
                    try {
                        C config = registrationRepository.findByProviderId(id);

                        if (config == null) {
                            throw new IllegalArgumentException("no configuration matching the given provider id");
                        }

                        logger.debug("build provider {} config", id);
                        return buildProvider(config);
                    } catch (ClassCastException ce) {
                        throw new IllegalArgumentException("no configuration matching the given provider id");
                    }
                }
            });

    public AbstractConfigurableProviderAuthority(
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

    protected abstract S buildProvider(C config);

    @Override
    public boolean hasProvider(String providerId) {
        C registration = registrationRepository.findByProviderId(providerId);
        return (registration != null);
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

            S p = providers.get(providerId);

            // check config version match against repo
            if (config.getVersion() > p.getConfig().getVersion()) {
                // invalidate cache and rebuild
                providers.invalidate(providerId);

                p = providers.get(providerId);
            }

            return p;
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
