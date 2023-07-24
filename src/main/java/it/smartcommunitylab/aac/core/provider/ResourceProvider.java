package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.Resource;

public interface ResourceProvider<R extends Resource> {
    /*
     * identify this provider
     */
    public String getAuthority();

    public String getProvider();

    public String getRealm();

    // TODO replace with proper typing <T> on resource
    public String getType();
}
