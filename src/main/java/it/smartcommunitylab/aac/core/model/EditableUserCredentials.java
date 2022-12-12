package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;

import it.smartcommunitylab.aac.SystemKeys;

public interface EditableUserCredentials extends EditableResource, UserResource, Serializable {
    default String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    // local id for provider
    public String getCredentialsId();

}
