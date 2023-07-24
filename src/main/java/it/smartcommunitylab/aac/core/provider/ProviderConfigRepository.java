package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import java.util.Collection;

public interface ProviderConfigRepository<T extends ProviderConfig<? extends ConfigMap>> {
    T findByProviderId(String providerId);

    Collection<T> findAll();

    Collection<T> findByRealm(String realm);

    void addRegistration(T registration);

    void removeRegistration(String providerId);

    void removeRegistration(T registration);
}
