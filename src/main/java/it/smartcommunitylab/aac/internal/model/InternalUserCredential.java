package it.smartcommunitylab.aac.internal.model;

import it.smartcommunitylab.aac.SystemKeys;

public interface InternalUserCredential {

    public CredentialsType getCredentialsType();

    public boolean isActive();

    default public String getAuthority() {
        return SystemKeys.AUTHORITY_INTERNAL;
    }
}
