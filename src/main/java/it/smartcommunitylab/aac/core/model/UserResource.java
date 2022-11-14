package it.smartcommunitylab.aac.core.model;

import it.smartcommunitylab.aac.SystemKeys;

/*
 * A realm scoped user resource, provided by an authority via a specific provider
 */

public interface UserResource extends Resource {

    public String getUserId();

    // uuid is global
    // TODO move to RegisteredResource
    default public String getUuid() {
        return null;
    }

    // resource is globally unique and addressable
    // ie given to an external actor he should be able to find the authority and
    // then the provider to request this resource
    @Override
    default public String getResourceId() {
        StringBuilder sb = new StringBuilder();
        sb.append(getAuthority()).append(SystemKeys.ID_SEPARATOR);
        sb.append(getProvider()).append(SystemKeys.ID_SEPARATOR);
        sb.append(getId());

        return sb.toString();
    }

    @Override
    default public String getUrn() {
        StringBuilder sb = new StringBuilder();
        sb.append(SystemKeys.URN_PROTOCOL);
        sb.append(getType()).append(SystemKeys.URN_SEPARATOR);
        sb.append(getResourceId());

        return sb.toString();
    }

    static String parseResourceIdForAuthority(String resourceId) {
        // explode resourceId
        String[] i = resourceId.split(SystemKeys.ID_SEPARATOR);
        if (i.length != 3) {
            throw new IllegalArgumentException("invalid id format");
        }
        return i[0];
    }

    static String parseResourceIdForProvider(String resourceId) {
        // explode resourceId
        String[] i = resourceId.split(SystemKeys.ID_SEPARATOR);
        if (i.length != 3) {
            throw new IllegalArgumentException("invalid id format");
        }
        return i[1];
    }

    static String parseResourceIdForId(String resourceId) {
        // explode resourceId
        String[] i = resourceId.split(SystemKeys.ID_SEPARATOR);
        if (i.length != 3) {
            throw new IllegalArgumentException("invalid id format");
        }
        return i[2];
    }
}
