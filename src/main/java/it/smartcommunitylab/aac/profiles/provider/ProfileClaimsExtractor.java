package it.smartcommunitylab.aac.profiles.provider;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import it.smartcommunitylab.aac.claims.base.AbstractClaim;
import it.smartcommunitylab.aac.claims.base.AbstractResourceClaimsExtractor;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.claims.AbstractProfileClaim;
import it.smartcommunitylab.aac.profiles.extractor.AttributeSetProfileExtractor;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import it.smartcommunitylab.aac.profiles.scope.AccountProfileScope;
import it.smartcommunitylab.aac.profiles.scope.BasicProfileScope;
import it.smartcommunitylab.aac.profiles.scope.EmailProfileScope;
import it.smartcommunitylab.aac.profiles.scope.ProfileResource;

public class ProfileClaimsExtractor extends AbstractResourceClaimsExtractor<ProfileResource> {

    Map<String, AbstractProfileClaimsExtractor<? extends AbstractProfileClaim<?>, ? extends AbstractProfile>> extractors;

    public ProfileClaimsExtractor(ProfileResource resource) {
        super(resource);

        extractors = new HashMap<>();
        // build core extractors
        extractors.put(AccountProfileScope.SCOPE, new AccountProfileClaimsExtractor(resource));
        extractors.put(EmailProfileScope.SCOPE, new EmailProfileClaimsExtractor(resource));
        extractors.put(BasicProfileScope.SCOPE, new BasicProfileClaimsExtractor(resource));

        // build extractors for all definitions
        Set<String> identifiers = resource.getIdentifiers();
        if (identifiers != null) {
            // use attribute set extractor
            // TODO make configurable to use attribute mapping
            identifiers.forEach(i -> extractors.put(i,
                    new CustomProfileClaimsExtractor(resource, new AttributeSetProfileExtractor(i))));
        }
    }

    @Override
    protected Collection<AbstractClaim> extractUserClaims(User user, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {

        if (scopes == null) {
            return null;
        }

        Set<AbstractClaim> claims = new HashSet<>();

        // build claims via extractors
        // note: we expect definitions to be consistent
        extractors.entrySet().forEach(e -> {
            if (scopes.contains(e.getKey())) {
                Collection<? extends AbstractProfileClaim<?>> pcs = e.getValue().extractProfileClaims(user, client,
                        scopes, extensions);
                if (pcs != null) {
                    claims.addAll(pcs);
                }
            }
        });

        return claims;
    }

    @Override
    protected Collection<AbstractClaim> extractClientClaims(ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {
        // not supported
        return null;
    }

}
