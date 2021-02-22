package it.smartcommunitylab.aac.core;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.model.UserIdentity;

public interface IdentityProvider {

    public UserIdentity getIdentity(String realm, String userId) throws NoSuchUserException;

    public UserIdentity getIdentity(String realm, String userId, boolean fetchAttributes) throws NoSuchUserException;

}
