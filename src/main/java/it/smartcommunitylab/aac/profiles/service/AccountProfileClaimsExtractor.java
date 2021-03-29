package it.smartcommunitylab.aac.profiles.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.claims.ClaimsSet;
import it.smartcommunitylab.aac.claims.DefaultClaimsSet;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;
import it.smartcommunitylab.aac.profiles.model.ProfileClaimsSet;

@Component
public class AccountProfileClaimsExtractor implements ScopeClaimsExtractor {

    public final static String NAMESPACE = "accounts";

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID;
    }

    @Override
    public String getScope() {
        return Config.SCOPE_ACCOUNT_PROFILE;
    }

    @Override
    public ClaimsSet extractUserClaims(UserDetails user, ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException {

        // we handle multiple profiles, one per identity
        List<AccountProfile> profiles = new ArrayList<>();

        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        for (UserIdentity identity : identities) {
            // get account and translate
            AccountProfile profile = identity.getAccount().toProfile();
            profiles.add(profile);
        }

        // convert to a serializable list
        ArrayList<Map<String, Serializable>> claims = new ArrayList<>();
        for (AccountProfile profile : profiles) {
            claims.add(profile.toMap());
        }

        // convert to map
        Map<String, Serializable> claimsMap = Collections.singletonMap(NAMESPACE, claims);

        // build a claimsSet
        DefaultClaimsSet claimsSet = new DefaultClaimsSet();
        claimsSet.setResourceId(getResourceId());
        claimsSet.setScope(getScope());
        // we merge our map with namespace to tld
        claimsSet.setNamespace(null);
        claimsSet.setUser(true);
        claimsSet.setClaims(claimsMap);

        return claimsSet;

    }

    @Override
    public ClaimsSet extractClientClaims(ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException {
        // not supported 
        return null;
    }

}
