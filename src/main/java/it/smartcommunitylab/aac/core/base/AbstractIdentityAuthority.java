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
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.authorities.IdentityProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.IdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;

public abstract class AbstractIdentityAuthority<I extends UserIdentity, S extends IdentityProvider<I>, C extends AbstractProviderConfig, P extends ConfigurableProperties>
        implements IdentityProviderAuthority<I, S, C, P>, InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final String authorityId;

    // user service
    protected final UserEntityService userEntityService;

    // resources registry
    protected final SubjectService subjectService;

    // configuration provider
    protected IdentityConfigurationProvider<C, P> configProvider;

    // identity provider configs by id
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
                    C config = registrationRepository.findByProviderId(id);

                    if (config == null) {
                        throw new IllegalArgumentException("no configuration matching the given provider id");
                    }

                    return buildProvider(config);
                }
            });

    public AbstractIdentityAuthority(
            String authorityId,
            UserEntityService userEntityService, SubjectService subjectService,
            ProviderConfigRepository<C> registrationRepository) {
        Assert.hasText(authorityId, "authority id  is mandatory");
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository is mandatory");

        this.authorityId = authorityId;
        this.userEntityService = userEntityService;
        this.subjectService = subjectService;
        this.registrationRepository = registrationRepository;
    }

    @Override
    public String getAuthorityId() {
        return authorityId;
    }

    @Override
    public FilterProvider getFilterProvider() {
        // authorities are not required to expose filters
        return null;
    }

    @Override
    public IdentityConfigurationProvider<C, P> getConfigurationProvider() {
        return configProvider;
    }

    protected abstract S buildProvider(C config);

    public void setConfigProvider(IdentityConfigurationProvider<C, P> configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(configProvider, "config provider is mandatory");
    }

    @Override
    public boolean hasProvider(String providerId) {
        C registration = registrationRepository.findByProviderId(providerId);
        return (registration != null);
    }

    @Override
    public S registerProvider(ConfigurableProvider cp) {
        // cast config and handle errors
        ConfigurableIdentityProvider cip = null;
        try {
            ConfigurableIdentityProvider c = (ConfigurableIdentityProvider) cp;
            cip = c;
        } catch (ClassCastException e) {
            logger.error("Wrong config class: " + e.getMessage());
            throw new IllegalArgumentException("unsupported config");
        }

        // we support only identity provider as resource providers
        if (cip != null
                && getAuthorityId().equals(cip.getAuthority())
                && SystemKeys.RESOURCE_IDENTITY.equals(cip.getType())) {
            String providerId = cip.getProvider();
            String realm = cip.getRealm();

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
                C providerConfig = configProvider.getConfig(cip);

                // register, we defer loading
                registrationRepository.addRegistration(providerConfig);

                // load and return
                return providers.get(providerId);
            } catch (Exception ex) {
                // cleanup
                registrationRepository.removeRegistration(providerId);

                throw new RegistrationException("invalid provider configuration: " + ex.getMessage(), ex);
            }
        } else {
            throw new IllegalArgumentException();
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

            // remove from cache
            providers.invalidate(providerId);

            // remove from registrations
            registrationRepository.removeRegistration(providerId);
        }
    }

    @Override
    public S getProvider(String providerId) {
        Assert.hasText(providerId, "provider id can not be null or empty");

        try {
            return providers.get(providerId);
        } catch (IllegalArgumentException | UncheckedExecutionException | ExecutionException e) {
            return null;
        }
    }

    @Override
    public List<S> getProviders(
            String realm) {
        // we need to fetch registrations and get idp from cache, with optional load
        Collection<C> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> getProvider(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }

}
