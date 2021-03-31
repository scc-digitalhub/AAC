package it.smartcommunitylab.aac.profiles.service;

import java.util.Collection;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;

@Component
public class OpenIdProfileExtractor extends UserProfileExtractor {

    @Override
    public OpenIdProfile extractUserProfile(User user)
            throws InvalidDefinitionException {

        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return null;
        }

        // TODO decide how to merge identities into a single profile
        // for now get first identity, should be last logged in
        OpenIdProfile profile = identities.iterator().next().toOpenIdProfile();

        return profile;
    }

}
