package it.smartcommunitylab.aac.profiles.provider;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.claims.CustomProfileClaim;
import it.smartcommunitylab.aac.profiles.extractor.UserProfileExtractor;
import it.smartcommunitylab.aac.profiles.model.CustomProfile;
import it.smartcommunitylab.aac.profiles.scope.AccountProfileScope;
import it.smartcommunitylab.aac.profiles.scope.ProfileResource;

public class CustomProfileClaimsExtractor extends AbstractProfileClaimsExtractor<CustomProfileClaim, CustomProfile> {
    private final UserProfileExtractor<CustomProfile> extractor;

    public CustomProfileClaimsExtractor(ProfileResource resource, UserProfileExtractor<CustomProfile> extractor) {
        super(resource);
        Assert.notNull(extractor, "extractor can not be null");
        this.extractor = extractor;
    }

    @Override
    public Collection<CustomProfileClaim> extractProfileClaims(User user, ClientDetails client,
            Collection<String> scopes,
            Map<String, Serializable> extensions) {

        // check if scope is present
        if (scopes == null || !scopes.contains(AccountProfileScope.SCOPE)) {
            return null;
        }

        try {
            // build profile via extractor
            Collection<CustomProfile> profiles = extractor.extractUserProfiles(user);
            List<CustomProfileClaim> claims = profiles.stream()
                    .map(p -> new CustomProfileClaim(p))
                    .collect(Collectors.toList());

            return claims;
        } catch (InvalidDefinitionException e) {
            return null;
        }

    }

}
