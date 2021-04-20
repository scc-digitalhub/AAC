package it.smartcommunitylab.aac.profiles.service;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

public class BasicProfileExtractor extends UserProfileExtractor {

    @Override
    public BasicProfile extractUserProfile(User user)
            throws InvalidDefinitionException {

        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return null;
        }

        // TODO decide how to merge identities into a single profile
        // for now get first identity, should be last logged in
        BasicProfile profile = identities.iterator().next().toBasicProfile();

        return profile;
    }

    @Override
    public BasicProfile extractUserProfile(UserIdentity identity) throws InvalidDefinitionException {
        if (identity == null) {
            return null;
        }

        return identity.toBasicProfile();
    }

    @Override
    public Collection<BasicProfile> extractUserProfiles(User user) throws InvalidDefinitionException {
        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return Collections.emptyList();
        }

        return identities.stream().map(i -> i.toBasicProfile()).collect(Collectors.toList());
    }

}
