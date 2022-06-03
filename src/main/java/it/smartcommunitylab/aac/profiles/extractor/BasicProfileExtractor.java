package it.smartcommunitylab.aac.profiles.extractor;

import java.util.Collection;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

public class BasicProfileExtractor extends AbstractUserProfileExtractor {

    @Override
    public String getIdentifier() {
        return BasicProfile.IDENTIFIER;
    }

    @Override
    public BasicProfile extractUserProfile(User user)
            throws InvalidDefinitionException {

        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return null;
        }

        // get first identity as base, should be last logged in
        UserIdentity id = identities.iterator().next();

        // get attributes merged with default policy
        Collection<UserAttributes> attributes = mergeAttributes(user, id.getId());

        // extract profile from user + attributes
        BasicProfile profile = extract(id.getAccount(), attributes);
        return profile;
    }

    public BasicProfile extractUserProfile(UserIdentity identity) throws InvalidDefinitionException {
        if (identity == null) {
            return null;
        }

        return extract(identity.getAccount(), identity.getAttributes());
    }

    private BasicProfile extract(UserAccount account, Collection<UserAttributes> attributes) {
        BasicProfile profile = new BasicProfile(account.getAuthority(), account.getProvider(), account.getRealm(),
                account.getUserId());

        // username is not modifiable via attributes
        profile.setUsername(account.getUsername());

        // email is not modifiable via attributes
        profile.setEmail(account.getEmailAddress());

        // lookup attributes with default names in basic profile
        String name = getStringAttribute(
                getAttribute(attributes, BasicAttributesSet.NAME, BasicAttributesSet.IDENTIFIER,
                        "profile"));
        if (!StringUtils.hasText(name)) {
            // fall back to openid profile
            name = getStringAttribute(getAttribute(attributes, OpenIdAttributesSet.GIVEN_NAME,
                    OpenIdAttributesSet.IDENTIFIER,
                    "profile"));
        }
        String surname = getStringAttribute(getAttribute(attributes, BasicAttributesSet.SURNAME,
                BasicAttributesSet.IDENTIFIER,
                "profile"));
        if (!StringUtils.hasText(surname)) {
            // fall back to openid profile
            surname = getStringAttribute(getAttribute(attributes, OpenIdAttributesSet.FAMILY_NAME,
                    OpenIdAttributesSet.IDENTIFIER,
                    "profile"));
        }

        profile.setName(name);
        profile.setSurname(surname);

        return profile;
    }

}
