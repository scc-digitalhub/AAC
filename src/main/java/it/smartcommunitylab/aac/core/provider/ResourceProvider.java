package it.smartcommunitylab.aac.core.provider;

public interface ResourceProvider {
    /*
     * identify this provider
     */
    public String getAuthority();

    public String getProvider();

    public String getRealm();

    // TODO replace with proper typing <T> on resource
    public String getType();

//    /*
//     * id mapping logic global <-> provider
//     */
//    public String exportInternalId(String internalId);
//
//    public String parseResourceId(String resourceId);
}
