package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.model.CredentialsType;
import it.smartcommunitylab.aac.model.Credentials;

public interface UserCredentials extends Credentials, UserResource, Serializable {

    boolean isChangeOnFirstAccess();

    CredentialsType getCredentialsType();

    default String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS + "_" + getCredentialsType().getValue();
    }

    default boolean isEditable() {
        return true;
    }
}
