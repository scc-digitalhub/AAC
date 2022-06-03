package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;

import it.smartcommunitylab.aac.SystemKeys;

/*
 * A profile describing a user, based on an attribute set with the same identifier
 */
public interface UserProfile extends UserResource, Serializable {

    default String getType() {
        return SystemKeys.RESOURCE_PROFILE;
    }

    default String getProfileId() {
        return getId();
    }
}
