package it.smartcommunitylab.aac.core.repository;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.base.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;

public class InMemoryProviderConfigRepository<U extends AbstractProviderConfig<?, ?>>
        implements ProviderConfigRepository<U> {

    private final Map<String, U> registrations;

    public InMemoryProviderConfigRepository() {
        this.registrations = new ConcurrentHashMap<>();
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
