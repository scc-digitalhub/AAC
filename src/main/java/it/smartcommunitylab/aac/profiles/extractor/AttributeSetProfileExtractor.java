package it.smartcommunitylab.aac.profiles.extractor;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.CustomProfile;

public class AttributeSetProfileExtractor extends AbstractUserProfileExtractor {

    // profile identifier, same as attribute set id
    private final String identifier;

    public AttributeSetProfileExtractor(String identifier) {
        Assert.hasText(identifier, "identifier can not be null or empty");
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public CustomProfile extractUserProfile(UserIdentity identity) throws InvalidDefinitionException {
        if (identity == null) {
            return null;
        }

        return extract(identity.getAccount(), identity.getAttributes());
    }

    @Override
    public CustomProfile extractUserProfile(User user) throws InvalidDefinitionException {
        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return null;
        }

        // TODO decide how to merge identities into a single profile
        // for now get first identity, should be last logged in
        UserIdentity id = identities.iterator().next();
        CustomProfile profile = extract(id.getAccount(), id.getAttributes());
        return profile;
    }

    @Override
    public Collection<? extends CustomProfile> extractUserProfiles(User user) throws InvalidDefinitionException {
        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return Collections.emptyList();
        }

        return identities.stream().map(id -> extract(id.getAccount(), id.getAttributes())).collect(Collectors.toList());

    }

    private CustomProfile extract(UserAccount account, Collection<UserAttributes> attributes) {
        CustomProfile profile = new CustomProfile(identifier);
        Optional<UserAttributes> attrs = attributes.stream().filter(a -> identifier.equals(a.getIdentifier()))
                .findFirst();
        if (attrs.isPresent()) {
            attrs.get().getAttributes().forEach(a -> {
                profile.addAttribute(a.getKey(), a.getValue());
            });
        }

        return profile;
    }
}
