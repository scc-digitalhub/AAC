package it.smartcommunitylab.aac.internal;

import java.util.Collection;
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
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.authorities.AttributeAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeService;
import it.smartcommunitylab.aac.internal.service.InternalAttributeEntityService;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeProviderConfig;

@Service
public class InternalAttributeAuthority implements AttributeAuthority {

    private final AttributeService attributeService;
    private final InternalAttributeEntityService attributeEntityService;

//  // attribute providers by id
//  // TODO move to a registry with cache/db etc
//  // this class should fetch only configuration from registry, parsed, and handle
//  // a loading cache to instantiate providers as needed

    private final ProviderRepository<InternalAttributeProviderConfig> registrationRepository;

    // loading cache for idps
    private final LoadingCache<String, InternalAttributeService> providers = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
            .maximumSize(100)
            .build(new CacheLoader<String, InternalAttributeService>() {
                @Override
                public InternalAttributeService load(final String id) throws Exception {
                    InternalAttributeProviderConfig config = registrationRepository.findByProviderId(id);

                    if (config == null) {
                        throw new IllegalArgumentException("no configuration matching the given provider id");
                    }

                    InternalAttributeService ap = new InternalAttributeService(
                            id,
                            attributeService, attributeEntityService,
                            config,
                            config.getRealm());
                    return ap;

                }
            });

    public InternalAttributeAuthority(
            AttributeService attributeService,
            InternalAttributeEntityService attributeEntityService,
            ProviderRepository<InternalAttributeProviderConfig> registrationRepository) {
        Assert.notNull(attributeService, "attribute service is mandatory");
        Assert.notNull(attributeEntityService, "attribute entity service is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository is mandatory");

        this.attributeService = attributeService;
        this.attributeEntityService = attributeEntityService;
        this.registrationRepository = registrationRepository;
    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_INTERNAL;
    }

    @Override
    public boolean hasAttributeProvider(String providerId) {
        InternalAttributeProviderConfig registration = registrationRepository.findByProviderId(providerId);
        return (registration != null);

    }

    @Override
    public InternalAttributeService getAttributeProvider(String providerId) {
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
        Collection<InternalAttributeProviderConfig> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> getAttributeProvider(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }

    @Override
    public InternalAttributeService registerAttributeProvider(ConfigurableAttributeProvider cp)
            throws IllegalArgumentException, RegistrationException, SystemException {
        if (cp != null
                && getAuthorityId().equals(cp.getAuthority())) {
            String providerId = cp.getProvider();
            String realm = cp.getRealm();

            // check if id clashes with another provider from a different realm
            InternalAttributeProviderConfig e = registrationRepository.findByProviderId(providerId);
            if (e != null && !realm.equals(e.getRealm())) {
                // name clash
                throw new RegistrationException("a provider with the same id already exists under a different realm");
            }

            try {
                InternalAttributeProviderConfig providerConfig = InternalAttributeProviderConfig
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
        InternalAttributeProviderConfig registration = registrationRepository.findByProviderId(providerId);
        if (registration != null) {
            // remove from cache
            providers.invalidate(providerId);

            // remove from registrations
            registrationRepository.removeRegistration(providerId);
        }
    }

    @Override
    public InternalAttributeService getAttributeService(String providerId) {
        return getAttributeProvider(providerId);
    }

    @Override
    public List<it.smartcommunitylab.aac.core.provider.AttributeService> getAttributeServices(String realm) {
        // we need to fetch registrations and get idp from cache, with optional load
        Collection<InternalAttributeProviderConfig> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> getAttributeProvider(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }

}
