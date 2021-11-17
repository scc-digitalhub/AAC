package it.smartcommunitylab.aac.spid.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.config.SpidProperties;
import it.smartcommunitylab.aac.spid.model.SpidRegistration;

public class LocalSpidRegistry implements SpidRegistry {

    private Map<String, SpidRegistration> idps;

    public LocalSpidRegistry(SpidProperties spidProperties) {
        this(spidProperties.getIdps());
    }

    public LocalSpidRegistry(Collection<SpidRegistration> idps) {
        Assert.notNull(idps, "identity providers can not be null");
        Map<String, SpidRegistration> idpMap = idps.stream().collect(Collectors.toMap(e -> e.getEntityId(), e -> e));
        this.idps = Collections.unmodifiableMap(idpMap);
    }

    @Override
    public Collection<SpidRegistration> getIdentityProviders() {
        return idps.values();
    }

    @Override
    public SpidRegistration getIdentityProvider(String entityId) {
        return idps.get(entityId);
    }

}
