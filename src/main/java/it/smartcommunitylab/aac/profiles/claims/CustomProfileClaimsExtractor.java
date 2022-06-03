package it.smartcommunitylab.aac.profiles.claims;

import java.util.Collection;
import java.util.Collections;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.base.AbstractProfile;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.extractor.UserProfileExtractor;

public class CustomProfileClaimsExtractor extends ProfileClaimsExtractor {
    private final UserProfileExtractor extractor;
    private final String identifier;

    public CustomProfileClaimsExtractor(UserProfileExtractor extractor) {
        Assert.notNull(extractor, "extractor can not be null");
        Assert.hasText(extractor.getIdentifier(), "identifier can not be null");
        this.extractor = extractor;
        this.identifier = extractor.getIdentifier();

    }

    private String getScope() {
        return "profile." + identifier + ".me";
    }

    @Override
    public Collection<String> getScopes() {
        return Collections.singleton(getScope());
    }

    @Override
    public String getKey() {
        // use key for custom profiles, enforce namespace
        return identifier;
    }

    @Override
    protected AbstractProfile buildUserProfile(User user, Collection<String> scopes)
            throws InvalidDefinitionException {

        AbstractProfile profile = (AbstractProfile) extractor.extractUserProfile(user);
        return profile;
    }

}