package it.smartcommunitylab.aac.attributes;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.provider.ScriptAttributeProvider;
import it.smartcommunitylab.aac.attributes.provider.ScriptAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.attributes.store.InMemoryAttributeStore;
import it.smartcommunitylab.aac.attributes.store.NullAttributeStore;
import it.smartcommunitylab.aac.attributes.store.PersistentAttributeStore;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.authorities.AttributeAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.ProviderRepository;

@Service
public class ScriptAttributeAuthority implements AttributeAuthority {

    private final AttributeService attributeService;

    // system attributes store
    private final AutoJdbcAttributeStore jdbcAttributeStore;

    // execution service for custom attributes mapping
    private final ScriptExecutionService executionService;

//  // attribute providers by id
//  // TODO move to a registry with cache/db etc
//  // this class should fetch only configuration from registry, parsed, and handle
//  // a loading cache to instantiate providers as needed

    private final ProviderRepository<ScriptAttributeProviderConfig> registrationRepository;

    // loading cache for idps
    private final LoadingCache<String, ScriptAttributeProvider> providers = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
            .maximumSize(100)
            .build(new CacheLoader<String, ScriptAttributeProvider>() {
                @Override
                public ScriptAttributeProvider load(final String id) throws Exception {
                    ScriptAttributeProviderConfig config = registrationRepository.findByProviderId(id);

                    if (config == null) {
                        throw new IllegalArgumentException("no configuration matching the given provider id");
                    }

                    AttributeStore attributeStore = getAttributeStore(id, config.getPersistence());

                    ScriptAttributeProvider ap = new ScriptAttributeProvider(
                            id,
                            attributeService, attributeStore,
                            config,
                            config.getRealm());
                    ap.setExecutionService(executionService);
                    return ap;

                }
            });

    public ScriptAttributeAuthority(
            AttributeService attributeService, ScriptExecutionService executionService,
            AutoJdbcAttributeStore jdbcAttributeStore,
            ProviderRepository<ScriptAttributeProviderConfig> registrationRepository) {
        Assert.notNull(attributeService, "attribute service is mandatory");
        Assert.notNull(executionService, "script execution service is mandatory");
        Assert.notNull(jdbcAttributeStore, "attribute store is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository is mandatory");

        this.attributeService = attributeService;
        this.executionService = executionService;
        this.jdbcAttributeStore = jdbcAttributeStore;
        this.registrationRepository = registrationRepository;
    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_SCRIPT;
    }

    @Override
    public boolean hasAttributeProvider(String providerId) {
        ScriptAttributeProviderConfig registration = registrationRepository.findByProviderId(providerId);
        return (registration != null);

    }

    @Override
    public ScriptAttributeProvider getAttributeProvider(String providerId) {
        Assert.hasText(providerId, "provider id can not be null or empty");
        try {
            return providers.get(providerId);
        } catch (IllegalArgumentException | UncheckedExecutionException | ExecutionException e) {
            return null;
        }
    }

    @Override
    public List<AttributeProvider> getAttributeProviders(String realm) {
        // we need to fetch registrations and get idp from cache, with optional load
        Collection<ScriptAttributeProviderConfig> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> getAttributeProvider(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }

    @Override
    public ScriptAttributeProvider registerAttributeProvider(ConfigurableAttributeProvider cp)
            throws IllegalArgumentException, RegistrationException, SystemException {
        if (cp != null
                && getAuthorityId().equals(cp.getAuthority())) {
            String providerId = cp.getProvider();
            String realm = cp.getRealm();

            // check if id clashes with another provider from a different realm
            ScriptAttributeProviderConfig e = registrationRepository.findByProviderId(providerId);
            if (e != null && !realm.equals(e.getRealm())) {
                // name clash
                throw new RegistrationException("a provider with the same id already exists under a different realm");
            }

            try {
                ScriptAttributeProviderConfig providerConfig = ScriptAttributeProviderConfig
                        .fromConfigurableProvider(cp);

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
    public void unregisterAttributeProvider(String providerId) throws SystemException {
        ScriptAttributeProviderConfig registration = registrationRepository.findByProviderId(providerId);
        if (registration != null) {
            // remove from cache
            providers.invalidate(providerId);

            // remove from registrations
            registrationRepository.removeRegistration(providerId);
        }
    }

    @Override
    public it.smartcommunitylab.aac.core.provider.AttributeService getAttributeService(String providerId) {
        return null;
    }

    @Override
    public List<it.smartcommunitylab.aac.core.provider.AttributeService> getAttributeServices(String realm) {
        return Collections.emptyList();
    }

    /*
     * helpers
     */

    private AttributeStore getAttributeStore(String providerId, String persistence) {
        // we generate a new store for each provider
        AttributeStore store = new NullAttributeStore();
        if (SystemKeys.PERSISTENCE_LEVEL_REPOSITORY.equals(persistence)) {
            store = new PersistentAttributeStore(getAuthorityId(), providerId, jdbcAttributeStore);
        } else if (SystemKeys.PERSISTENCE_LEVEL_MEMORY.equals(persistence)) {
            store = new InMemoryAttributeStore(getAuthorityId(), providerId);
        }

        return store;
    }

}
