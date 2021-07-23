package it.smartcommunitylab.aac.spid.service;

import java.util.Collection;

import it.smartcommunitylab.aac.spid.model.SpidRegistration;

public interface SpidRegistry {

    public Collection<SpidRegistration> getIdentityProviders();

    public SpidRegistration getIdentityProvider(String entityId);

}
