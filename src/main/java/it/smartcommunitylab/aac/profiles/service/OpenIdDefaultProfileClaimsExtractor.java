package it.smartcommunitylab.aac.profiles.service;

import java.util.Collection;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;

@Component
public class OpenIdDefaultProfileClaimsExtractor extends ProfileClaimsExtractor {

    @Override
    public String getScope() {
        return Config.SCOPE_PROFILE;
    }

    @Override
    protected AbstractProfile buildUserProfile(UserDetails user, Collection<String> scopes)
            throws InvalidDefinitionException {

        if (!scopes.contains(Config.SCOPE_OPENID)) {
            return null;
        }

        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            throw new InvalidDefinitionException("no identities found");
        }

        // TODO decide how to merge identities into a single profile
        // for now get first identity, should be last logged in
        OpenIdProfile profile = identities.iterator().next().toOpenIdProfile();

        // narrow down
        return profile.toDefaultProfile();
    }

}
