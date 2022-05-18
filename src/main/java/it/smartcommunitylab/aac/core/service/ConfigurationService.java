package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.base.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;

@Service
public class ConfigurationService {

    private final Map<String, Map<String, ConfigurationProvider<? extends ConfigurableProvider, ? extends AbstractProviderConfig, ? extends ConfigurableProperties>>> providers;

    public ConfigurationService(
            Collection<ConfigurationProvider<? extends ConfigurableProvider, ? extends AbstractProviderConfig, ? extends ConfigurableProperties>> providers) {

        Map<String, Map<String, ConfigurationProvider<? extends ConfigurableProvider, ? extends AbstractProviderConfig, ? extends ConfigurableProperties>>> map = new HashMap<>();
        Set<String> types = providers.stream().map(p -> p.getType()).collect(Collectors.toSet());

        for (String type : types) {
            Map<String, ConfigurationProvider<? extends ConfigurableProvider, ? extends AbstractProviderConfig, ? extends ConfigurableProperties>> mapTypes = providers
                    .stream()
                    .filter(p -> p.getType().equals(type))
                    .collect(Collectors.toMap(e -> e.getAuthority(), e -> e));
            map.put(type, mapTypes);
        }

        this.providers = map;

    }

    public ConfigurationProvider<? extends ConfigurableProvider, ? extends AbstractProviderConfig, ? extends ConfigurableProperties> getProvider(
            String type, String authority) throws NoSuchProviderException {
        if (!providers.containsKey(type) || !providers.get(type).containsKey(authority)) {
            throw new NoSuchProviderException();
        }

        return providers.get(type).get(authority);

    }

    public Collection<ConfigurationProvider<? extends ConfigurableProvider, ? extends AbstractProviderConfig, ? extends ConfigurableProperties>> listProviders(
            String type) {
        return providers.getOrDefault(type, Collections.emptyMap()).values();
    }
}
