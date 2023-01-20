package it.smartcommunitylab.aac.core.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.base.AbstractConfigMap;
import it.smartcommunitylab.aac.core.base.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;

/*
 * A provider config repository which creates missing configs on-the-fly via factories
 */
public class AutoProviderConfigRepository<C extends AbstractProviderConfig<M, T>, M extends AbstractConfigMap, T extends ConfigurableProvider>
        implements ProviderConfigRepository<C> {

    private final ProviderConfigRepository<C> repository;

    // creator builds a new config from providerId
    private Function<String, C> creator = (providerId) -> (null);

    // factory builds a new config from realm
    private Function<String, C> factory = (realm) -> (null);

    public AutoProviderConfigRepository(ProviderConfigRepository<C> baseRepository) {
        Assert.notNull(baseRepository, "base repository can not be null");

        this.repository = baseRepository;
    };

    public void setCreator(Function<String, C> creator) {
        this.creator = creator;
    }

    public void setFactory(Function<String, C> factory) {
        this.factory = factory;
    }

    @Override
    public C findByProviderId(String providerId) {
        if (providerId == null) {
            throw new IllegalArgumentException();
        }

        C c = repository.findByProviderId(providerId);
        if (c == null) {
            // use creator and store if successful
            c = creator.apply(providerId);
            if (c != null) {
                repository.addRegistration(c);
            }
        }

        return c;
    }

    @Override
    public Collection<C> findAll() {
        return repository.findAll();
    }

    @Override
    public Collection<C> findByRealm(String realm) {
        if (realm == null) {
            throw new IllegalArgumentException();
        }

        Collection<C> list = repository.findByRealm(realm);
        if (list.isEmpty()) {
            // use factory and store if successful
            C c = factory.apply(realm);
            if (c != null) {
                repository.addRegistration(c);

                return Collections.singleton(c);
            }
        }

        return list;
    }

    @Override
    public void addRegistration(C registration) {
        repository.addRegistration(registration);
    }

    @Override
    public void removeRegistration(String providerId) {
        repository.removeRegistration(providerId);
    }

    @Override
    public void removeRegistration(C registration) {
        repository.removeRegistration(registration);
    }

}
