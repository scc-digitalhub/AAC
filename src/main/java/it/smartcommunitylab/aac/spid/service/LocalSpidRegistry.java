package it.smartcommunitylab.aac.spid.service;

import it.smartcommunitylab.aac.config.SpidProperties;
import it.smartcommunitylab.aac.spid.model.SpidIdPRegistration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalSpidRegistry implements SpidRegistry {

    private final Map<String, SpidIdPRegistration> identityProviders;

    public LocalSpidRegistry() {
        identityProviders = new HashMap<>();
    }

    public LocalSpidRegistry(SpidProperties properties) {
        this(properties.getIdentityProviders());
    }

    public LocalSpidRegistry(Collection<SpidIdPRegistration> idps) {
        Map<String, SpidIdPRegistration> registryMap = idps
            .stream()
            .collect(Collectors.toMap(SpidIdPRegistration::getEntityId, e -> e));
        identityProviders = Collections.unmodifiableMap(registryMap);
    }

    @Override
    public Collection<SpidIdPRegistration> getIdentityProviders() {
        return identityProviders.values();
    }

    @Override
    public SpidIdPRegistration getIdentityProvider(String entityId) {
        return identityProviders.get(entityId);
    }
}
