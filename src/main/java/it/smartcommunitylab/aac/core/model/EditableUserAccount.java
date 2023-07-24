package it.smartcommunitylab.aac.core.model;

import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;

/*
 * An editable account, suitable for registration
 */

public interface EditableUserAccount extends EditableResource, UserResource, Serializable {
    default String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    // accountId is local id for provider
    public String getAccountId();
}
