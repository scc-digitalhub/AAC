package it.smartcommunitylab.aac.profiles.claims;

import java.util.Collection;
import java.util.Collections;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;
import it.smartcommunitylab.aac.profiles.service.OpenIdProfileExtractor;

public class OpenIdEmailProfileClaimsExtractor extends ProfileClaimsExtractor {

    private final OpenIdProfileExtractor extractor;

    public OpenIdEmailProfileClaimsExtractor() {
        this.extractor = new OpenIdProfileExtractor();
    }

    @Override
    public Collection<String> getScopes() {
        return Collections.singleton(Config.SCOPE_EMAIL);
    }

    @Override
    public String getKey() {
        // no key for email, we merge to TLD
        return null;
    }

    @Override
    protected AbstractProfile buildUserProfile(User user, Collection<String> scopes)
            throws InvalidDefinitionException {

        if (!scopes.contains(Config.SCOPE_OPENID)) {
            return null;
        }

        OpenIdProfile profile = extractor.extractUserProfile(user);

        // narrow down
        return profile.toEmailProfile();
    }

}
