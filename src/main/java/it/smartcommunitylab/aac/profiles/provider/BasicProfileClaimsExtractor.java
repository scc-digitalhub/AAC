package it.smartcommunitylab.aac.profiles.provider;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.claims.BasicProfileClaim;
import it.smartcommunitylab.aac.profiles.extractor.BasicProfileExtractor;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.profiles.scope.EmailProfileScope;
import it.smartcommunitylab.aac.profiles.scope.ProfileResource;

public class BasicProfileClaimsExtractor extends AbstractProfileClaimsExtractor<BasicProfileClaim, BasicProfile> {
    private final BasicProfileExtractor extractor;

    public BasicProfileClaimsExtractor(ProfileResource resource) {
        super(resource);
        this.extractor = new BasicProfileExtractor();
    }

    @Override
    public Collection<BasicProfileClaim> extractProfileClaims(User user, ClientDetails client,
            Collection<String> scopes,
            Map<String, Serializable> extensions) {

        // check if scope is present
        if (scopes == null || !scopes.contains(EmailProfileScope.SCOPE)) {
            return null;
        }

        try {
            // build profile via extractor, single
            BasicProfile profile = extractor.extractUserProfile(user);
            if (profile != null) {
                return Collections.singletonList(new BasicProfileClaim(profile));
            }

            return Collections.emptyList();
        } catch (InvalidDefinitionException e) {
            return null;
        }

    }

}
