package it.smartcommunitylab.aac.profiles.service;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;

public abstract class UserProfileExtractor {

    public abstract AbstractProfile extractUserProfile(User user)
            throws InvalidDefinitionException;
}
