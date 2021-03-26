package it.smartcommunitylab.aac.profiles.service;

import java.util.Collection;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

public class BasicProfileClaimsExtractor extends ProfileClaimsExtractor {

    @Override
    public String getScope() {
        return Config.SCOPE_BASIC_PROFILE;
    }

    @Override
    protected AbstractProfile buildUserProfile(UserDetails user, Collection<String> scopes)
            throws InvalidDefinitionException {
        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            throw new InvalidDefinitionException("no identities found");
        }

        // TODO decide how to merge identities into a single profile
        // for now get first identity, should be last logged in
        BasicProfile profile = identities.iterator().next().toBasicProfile();

        return profile;
    }

}