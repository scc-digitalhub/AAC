package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;

import it.smartcommunitylab.aac.SystemKeys;

/*
 * An editable account, suitable for registration
 */

public interface EditableUserAccount extends UserResource, Serializable {
    default String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    // accountId is local id for provider
    public String getAccountId();

    default String getResourceId() {
        return getAccountId();
    }

}
