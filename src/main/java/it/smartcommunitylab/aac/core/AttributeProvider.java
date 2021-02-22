package it.smartcommunitylab.aac.core;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.model.UserAttributes;

public interface AttributeProvider {

    public UserAttributes getAttributes(String realm, String userId) throws NoSuchUserException;

}
