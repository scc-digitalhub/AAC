package it.smartcommunitylab.aac.core.model;

/*
 * A realm scoped resource, provided by an authority
 */

public interface Resource {
    public String getRealm();

    public String getAuthority();

    public String getProvider();

    // id is global across all resources (uuid)
    public String getId();

    // TODO replace with proper typing <T> on resource
    public String getType();

    // resourceId is local to (type)/authority+provider
    public String getResourceId();

    public default String getUrn() {
        return null;
    }
}
