package it.smartcommunitylab.aac.profiles.provider;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.claims.EmailsProfileClaim;
import it.smartcommunitylab.aac.profiles.extractor.EmailProfileExtractor;
import it.smartcommunitylab.aac.profiles.model.EmailProfile;
import it.smartcommunitylab.aac.profiles.scope.EmailProfileScope;
import it.smartcommunitylab.aac.profiles.scope.ProfileResource;

public class EmailProfileClaimsExtractor extends AbstractProfileClaimsExtractor<EmailsProfileClaim, EmailProfile> {
    private final EmailProfileExtractor extractor;

    public EmailProfileClaimsExtractor(ProfileResource resource) {
        super(resource);
        this.extractor = new EmailProfileExtractor();
    }

    @Override
    public Collection<EmailsProfileClaim> extractProfileClaims(User user, ClientDetails client,
            Collection<String> scopes,
            Map<String, Serializable> extensions) {

        // check if scope is present
        if (scopes == null || !scopes.contains(EmailProfileScope.SCOPE)) {
            return null;
        }

        try {
            // build profile via extractor
            Collection<EmailProfile> profiles = extractor.extractUserProfiles(user);
            List<EmailsProfileClaim> claims = profiles.stream()
                    .map(p -> new EmailsProfileClaim(p))
                    .collect(Collectors.toList());

            return claims;
        } catch (InvalidDefinitionException e) {
            return null;
        }

    }

}
