package it.smartcommunitylab.aac.profiles.provider;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.claims.AccountsProfileClaim;
import it.smartcommunitylab.aac.profiles.extractor.AccountProfileExtractor;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;
import it.smartcommunitylab.aac.profiles.scope.AccountProfileScope;
import it.smartcommunitylab.aac.profiles.scope.ProfileResource;

public class AccountProfileClaimsExtractor
        extends AbstractProfileClaimsExtractor<AccountsProfileClaim, AccountProfile> {
    private final AccountProfileExtractor extractor;

    public AccountProfileClaimsExtractor(ProfileResource resource) {
        super(resource);
        this.extractor = new AccountProfileExtractor();
    }

    @Override
    public Collection<AccountsProfileClaim> extractProfileClaims(User user, ClientDetails client,
            Collection<String> scopes,
            Map<String, Serializable> extensions) {

        // check if scope is present
        if (scopes == null || !scopes.contains(AccountProfileScope.SCOPE)) {
            return null;
        }

        try {
            // build profile via extractor
            Collection<AccountProfile> profiles = extractor.extractUserProfiles(user);
            List<AccountsProfileClaim> claims = profiles.stream()
                    .map(p -> new AccountsProfileClaim(p))
                    .collect(Collectors.toList());

            return claims;
        } catch (InvalidDefinitionException e) {
            return null;
        }

    }

}
