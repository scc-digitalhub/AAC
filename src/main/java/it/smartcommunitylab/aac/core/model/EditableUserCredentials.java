package it.smartcommunitylab.aac.core.model;

import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;

public interface EditableUserCredentials extends EditableResource, UserResource, Serializable {
    default String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    // local id for provider
    public String getCredentialsId();
}
