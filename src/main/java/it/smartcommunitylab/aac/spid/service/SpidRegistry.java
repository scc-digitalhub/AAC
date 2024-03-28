package it.smartcommunitylab.aac.spid.service;

import it.smartcommunitylab.aac.spid.model.SpidIdPRegistration;

import java.util.Collection;

/*
 * SpidRegistry is a registry of SPID certified identity providers, such as Infocert, Lepida, Poste, etc.
 * For an up to date list of certified providers, see the page
 *      https://registry.spid.gov.it/identity-providers
 */
public interface SpidRegistry {

    public Collection<SpidIdPRegistration> getIdentityProviders();

    public SpidIdPRegistration getIdentityProvider(String entityId);

}
