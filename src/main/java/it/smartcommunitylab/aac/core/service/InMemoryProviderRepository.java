package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;

public class InMemoryProviderRepository<U extends ResourceProvider> implements ProviderRepository<U> {

    private final Map<String, U> registrations;

    public InMemoryProviderRepository() {
        this.registrations = new HashMap<>();
    }

    @Override
    public U findByProviderId(String providerId) {
        Assert.hasText(providerId, "providerId cannot be empty");
        return registrations.get(providerId);
    }

    @Override
    public void addRegistration(U registration) {
        registrations.put(registration.getProvider(), registration);
    }

    @Override
    public void removeRegistration(String providerId) {
        registrations.remove(providerId);
    }

    @Override
    public void removeRegistration(U registration) {
        registrations.remove(registration.getProvider());
    }

    @Override
    public Collection<U> findAll() {
        return registrations.values();
    }

    @Override
    public Collection<U> findByRealm(String realm) {
        return registrations.values().stream().filter(p -> p.getRealm().equals(realm)).collect(Collectors.toList());
    }

}
