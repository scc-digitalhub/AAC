package it.smartcommunitylab.aac.profiles.service;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

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

    @Override
    public OpenIdProfile extractUserProfile(UserIdentity identity) throws InvalidDefinitionException {
        if (identity == null) {
            return null;
        }

        return identity.toOpenIdProfile();
    }

    @Override
    public Collection<OpenIdProfile> extractUserProfiles(User user) throws InvalidDefinitionException {
        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return Collections.emptyList();
        }

        return identities.stream().map(i -> i.toOpenIdProfile()).collect(Collectors.toList());
    }

}
