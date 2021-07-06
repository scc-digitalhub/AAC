package it.smartcommunitylab.aac.profiles.extractor;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

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
    public BasicProfile extractUserProfile(User user)
            throws InvalidDefinitionException {

        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return null;
        }

        // TODO decide how to merge identities into a single profile
        // for now get first identity, should be last logged in
        UserIdentity id = identities.iterator().next();
        BasicProfile profile = extract(id.getAccount(), id.getAttributes());
        return profile;
    }

    @Override
    public BasicProfile extractUserProfile(UserIdentity identity) throws InvalidDefinitionException {
        if (identity == null) {
            return null;
        }

        return extract(identity.getAccount(), identity.getAttributes());
    }

    @Override
    public Collection<BasicProfile> extractUserProfiles(User user) throws InvalidDefinitionException {
        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return Collections.emptyList();
        }

        return identities.stream().map(id -> extract(id.getAccount(), id.getAttributes())).collect(Collectors.toList());
    }

    private BasicProfile extract(UserAccount account, Collection<UserAttributes> attributes) {
        BasicProfile profile = new BasicProfile();

        // username is not modifiable via attributes
        profile.setUsername(account.getUsername());

        // lookup attributes with default names in basic profile
        String name = getStringAttribute(
                getAttribute(attributes, BasicAttributesSet.NAME, BasicAttributesSet.IDENTIFIER,
                        "profile", "profile.fullprofile.me"));
        if (!StringUtils.hasText(name)) {
            // fall back to openid profile
            name = getStringAttribute(getAttribute(attributes, OpenIdAttributesSet.GIVEN_NAME,
                    OpenIdAttributesSet.IDENTIFIER,
                    "profile", "profile.fullprofile.me"));
        }
        String surname = getStringAttribute(getAttribute(attributes, BasicAttributesSet.SURNAME,
                BasicAttributesSet.IDENTIFIER,
                "profile", "profile.fullprofile.me"));
        if (!StringUtils.hasText(surname)) {
            // fall back to openid profile
            surname = getStringAttribute(getAttribute(attributes, OpenIdAttributesSet.FAMILY_NAME,
                    OpenIdAttributesSet.IDENTIFIER,
                    "profile", "profile.fullprofile.me"));
        }
        String email = getStringAttribute(
                getAttribute(attributes, BasicAttributesSet.EMAIL, BasicAttributesSet.IDENTIFIER,
                        "profile", "profile.fullprofile.me"));
        if (!StringUtils.hasText(email)) {
            // fall back to openid profile
            email = getStringAttribute(
                    getAttribute(attributes, OpenIdAttributesSet.EMAIL, OpenIdAttributesSet.IDENTIFIER,
                            "profile", "profile.fullprofile.me"));
        }

        profile.setName(name);
        profile.setSurname(surname);
        profile.setEmail(email);

        return profile;
    }

}
