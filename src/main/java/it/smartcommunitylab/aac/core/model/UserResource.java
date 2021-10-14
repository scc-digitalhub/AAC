package it.smartcommunitylab.aac.core.model;

/*
 * A realm scoped user resource, provided by an authority via a specific provider
 */

public interface UserResource {

    public String getRealm();

    public String getAuthority();

    public String getProvider();
    
    // TODO replace with proper typing <T> on resource
    public String getType();

    // userid is globally unique and addressable
    // ie given to an external actor he should be able to find the authority and
    // then the provider to request this resource
    public String getUserId();

}
