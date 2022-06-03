package it.smartcommunitylab.aac.profiles.extractor;

import java.util.Collection;
import java.util.Optional;
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

        // get first identity as base, should be last logged in
        UserIdentity id = identities.iterator().next();

        // get attributes merged with default policy
        Collection<UserAttributes> attributes = mergeAttributes(user, id.getId());

        // fetch attribute from merged as-is
        CustomProfile profile = extract(id.getAccount(), attributes);
        return profile;
    }

    private CustomProfile extract(UserAccount account, Collection<UserAttributes> attributes) {
        CustomProfile profile = new CustomProfile(account.getAuthority(), account.getProvider(), account.getRealm(),
                account.getUserId(), identifier);

        Optional<UserAttributes> attrs = attributes.stream().filter(a -> identifier.equals(a.getIdentifier()))
                .findFirst();
        if (attrs.isPresent()) {
            attrs.get().getAttributes().forEach(a -> {
                profile.addAttribute(a.getKey(), a.exportValue());
            });
        }

        return profile;
    }
}
