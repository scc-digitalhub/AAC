package it.smartcommunitylab.aac.core.model;

/*
 * A realm scoped resource, provided by an authority 
 */

public interface Resource {

    public String getRealm();

    public String getAuthority();

    public String getProvider();

    // id is local to the provider
    public String getId();

    // uuid is global
    // TODO move to RegisteredResource
    default public String getUuid() {
        return null;
    }

    // TODO replace with proper typing <T> on resource
    public String getType();

    // resource is globally unique and addressable
    // ie given to an external actor he should be able to find the authority and
    // then the provider to request this resource
    // TODO move to AddressableResource
    default public String getResourceId() {
        return null;
    }

    default public String getUrn() {
        return null;
    }
}
