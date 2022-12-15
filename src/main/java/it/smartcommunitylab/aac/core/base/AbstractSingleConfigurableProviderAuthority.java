package it.smartcommunitylab.aac.core.base;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.authorities.SingleProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.Resource;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;

public abstract class AbstractSingleConfigurableProviderAuthority<S extends ConfigurableResourceProvider<R, T, M, C>, R extends Resource, T extends ConfigurableProvider, M extends AbstractConfigMap, C extends AbstractProviderConfig<M, T>>
        extends AbstractConfigurableProviderAuthority<S, R, T, M, C> implements SingleProviderAuthority<S, R, T, M, C> {
    public AbstractSingleConfigurableProviderAuthority(
            String authorityId,
            ProviderConfigRepository<C> registrationRepository) {
        super(authorityId, registrationRepository);
    }

    @Override
    public C registerProvider(ConfigurableProvider cp) throws RegistrationException {
        if (cp != null
                && getAuthorityId().equals(cp.getAuthority())) {
            // enforce single per realm
            String realm = cp.getRealm();

            // check if there is already a provider for realm (except the current)
            Collection<C> list = registrationRepository.findByRealm(realm);
            if (list.size() > 1) {
                throw new RegistrationException("a provider already exists in the same realm");
            } else if (list.size() == 1 && !list.iterator().next().getProvider().equals(cp.getProvider())) {
                throw new RegistrationException("a provider already exists in the same realm");
            }

            // register
            return super.registerProvider(cp);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public S findProviderByRealm(String realm) {
        // we need to fetch registrations and get idp from cache, with optional load
        // we expect a single provider per realm, so fetch first
        Collection<C> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> findProvider(r.getProvider()))
                .filter(p -> (p != null)).findFirst().orElse(null);

    }

    @Override
    public S getProviderByRealm(String realm) throws NoSuchProviderException {
        // fetch first if available
        S provider = findProviderByRealm(realm);

        if (provider == null) {
            throw new NoSuchProviderException();
        }

        return provider;
    }

    @Override
    public List<S> getProvidersByRealm(String realm) {
        // fetch first if available
        S provider = findProviderByRealm(realm);
        if (provider == null) {
            return Collections.emptyList();
        }

        return Collections.singletonList(provider);
    }
}
