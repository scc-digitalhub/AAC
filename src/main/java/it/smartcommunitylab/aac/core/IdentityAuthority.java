package it.smartcommunitylab.aac.core;

import java.util.List;

import it.smartcommunitylab.aac.core.provider.IdentityProvider;

public interface IdentityAuthority {

    /*
     * identify
     */
    public String getAuthorityId();

    // identity

    public IdentityProvider getIdentityProvider(String providerId);

    public List<IdentityProvider> getIdentityProviders(String realm);

    /*
     * we also expect auth provider to be able to infer *provider from userId
     * implementations should build ids predictably via composition
     * 
     * also *providers should return the same id for the same user!
     */
    public IdentityProvider getUserIdentityProvider(String userId);

}
