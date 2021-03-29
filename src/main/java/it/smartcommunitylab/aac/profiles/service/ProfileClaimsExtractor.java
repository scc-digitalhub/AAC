package it.smartcommunitylab.aac.profiles.service;

import java.util.Collection;

import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.claims.ClaimsSet;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import it.smartcommunitylab.aac.profiles.model.ProfileClaimsSet;

public abstract class ProfileClaimsExtractor implements ScopeClaimsExtractor {

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID;
    }

    @Override
    public abstract String getScope();

    @Override
    public ClaimsSet extractUserClaims(UserDetails user, ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException {

        AbstractProfile profile = buildUserProfile(user, scopes);

        // build a claimsSet
        ClaimsSet claimsSet = buildClaimsSet(profile, true);

        return claimsSet;

    }

    // subclasses need to provide the profile
    protected abstract AbstractProfile buildUserProfile(UserDetails user, Collection<String> scopes)
            throws InvalidDefinitionException;

    protected ClaimsSet buildClaimsSet(AbstractProfile profile, boolean isUser) {
        ProfileClaimsSet claimsSet = new ProfileClaimsSet();
        claimsSet.setScope(getScope());

        // by default profile claims are top level
        claimsSet.setNamespace(null);

        // set profile
        claimsSet.setUser(isUser);
        claimsSet.setProfile(profile);

        return claimsSet;
    }

    @Override
    public ClaimsSet extractClientClaims(ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException {
        // not supported now but subclasses can override
        return null;
    }

}
