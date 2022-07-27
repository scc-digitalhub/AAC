package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.Credentials;

public interface UserCredentials extends Credentials, UserResource, Serializable {

    boolean isChangeOnFirstAccess();

    default String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    default boolean isEditable() {
        return true;
    }
}
