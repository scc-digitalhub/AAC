package it.smartcommunitylab.aac.profiles.claims;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.claims.Claim;
import it.smartcommunitylab.aac.claims.ClaimsSet;
import it.smartcommunitylab.aac.claims.DefaultClaimsSet;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.claims.model.SerializableClaim;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.extractor.AccountProfileExtractor;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AccountProfileClaimsExtractor implements ScopeClaimsExtractor {

    public static final String NAMESPACE = "accounts";

    private final AccountProfileExtractor extractor;

    public AccountProfileClaimsExtractor() {
        this.extractor = new AccountProfileExtractor();
    }

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID;
    }

    @Override
    public Collection<String> getScopes() {
        return Collections.singleton(Config.SCOPE_ACCOUNT_PROFILE);
    }

    @Override
    public ClaimsSet extractUserClaims(
        String scope,
        User user,
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    ) throws InvalidDefinitionException, SystemException {
        // we handle multiple profiles, one per identity
        Collection<AccountProfile> profiles = extractor.extractUserProfiles(user);

        // convert to a claims list
        List<Claim> claims = new ArrayList<>();
        for (AccountProfile profile : profiles) {
            SerializableClaim claim = new SerializableClaim("accounts");
            claim.setValue(profile.toMap());
            claims.add(claim);
        }

        // build a claimsSet
        DefaultClaimsSet claimsSet = new DefaultClaimsSet();
        claimsSet.setResourceId(getResourceId());
        claimsSet.setScope(Config.SCOPE_ACCOUNT_PROFILE);
        // we merge our map with namespace to tld
        claimsSet.setNamespace(null);
        claimsSet.setUser(true);
        claimsSet.setClaims(claims);

        return claimsSet;
    }

    @Override
    public ClaimsSet extractClientClaims(
        String scope,
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    ) throws InvalidDefinitionException, SystemException {
        // not supported
        return null;
    }

    @Override
    public String getRealm() {
        return null;
    }
}
