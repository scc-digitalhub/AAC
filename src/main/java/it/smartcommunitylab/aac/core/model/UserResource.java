package it.smartcommunitylab.aac.core.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.repository.JsonSchemaIgnore;

/*
 * A realm scoped user resource, provided by an authority via a specific provider
 */

public interface UserResource extends Resource {

    public String getUserId();

    // uuid is global
    public String getUuid();

    @JsonSchemaIgnore
    default public String getUrn() {
        StringBuilder sb = new StringBuilder();
        sb.append(SystemKeys.URN_PROTOCOL);
        sb.append(getType()).append(SystemKeys.URN_SEPARATOR);
        sb.append(getAuthority()).append(SystemKeys.ID_SEPARATOR);
        sb.append(getProvider()).append(SystemKeys.ID_SEPARATOR);
        sb.append(getId());

        return sb.toString();
    }

}
