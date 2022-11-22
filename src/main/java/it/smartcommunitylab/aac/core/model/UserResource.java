package it.smartcommunitylab.aac.core.model;

import it.smartcommunitylab.aac.SystemKeys;

/*
 * A realm scoped user resource, provided by an authority via a specific provider
 */

public interface UserResource extends Resource {

    public String getUserId();

    // uuid is global
    public String getUuid();

    @Override
    default public String getUrn() {
        StringBuilder sb = new StringBuilder();
        sb.append(SystemKeys.URN_PROTOCOL);
        sb.append(getType()).append(SystemKeys.URN_SEPARATOR);
        sb.append(getAuthority()).append(SystemKeys.ID_SEPARATOR);
        sb.append(getProvider()).append(SystemKeys.ID_SEPARATOR);
        sb.append(getResourceId());

        return sb.toString();
    }

}
