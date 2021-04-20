package it.smartcommunitylab.aac.profiles.service;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;

public class AccountProfileExtractor extends UserProfileExtractor {

    @Override
    public AccountProfile extractUserProfile(User user)
            throws InvalidDefinitionException {

        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return null;
        }

        // TODO decide how to select primary identity
        // for now get first identity, should be last logged in
        AccountProfile profile = identities.iterator().next().getAccount().toProfile();

        return profile;
    }

    @Override
    public AccountProfile extractUserProfile(UserIdentity identity) throws InvalidDefinitionException {
        if (identity == null) {
            return null;
        }

        return identity.getAccount().toProfile();
    }

    @Override
    public Collection<AccountProfile> extractUserProfiles(User user) throws InvalidDefinitionException {
        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return Collections.emptyList();
        }

        return identities.stream().map(i -> i.getAccount().toProfile()).collect(Collectors.toList());
    }

}
