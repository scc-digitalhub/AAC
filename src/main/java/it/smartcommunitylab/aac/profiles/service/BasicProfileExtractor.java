package it.smartcommunitylab.aac.profiles.service;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.BasicProfileAttributesSet;
import it.smartcommunitylab.aac.profiles.OpenIdProfileAttributesSet;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

public class BasicProfileExtractor extends UserProfileExtractor {

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
        String name = getAttribute(attributes, BasicProfileAttributesSet.NAME, BasicProfileAttributesSet.IDENTIFIER,
                "profile", "profile.fullprofile.me");
        if (!StringUtils.hasText(name)) {
            // fall back to openid profile
            name = getAttribute(attributes, OpenIdProfileAttributesSet.GIVEN_NAME,
                    OpenIdProfileAttributesSet.IDENTIFIER,
                    "profile", "profile.fullprofile.me");
        }
        String surname = getAttribute(attributes, BasicProfileAttributesSet.SURNAME,
                BasicProfileAttributesSet.IDENTIFIER,
                "profile", "profile.fullprofile.me");
        if (!StringUtils.hasText(surname)) {
            // fall back to openid profile
            surname = getAttribute(attributes, OpenIdProfileAttributesSet.FAMILY_NAME,
                    OpenIdProfileAttributesSet.IDENTIFIER,
                    "profile", "profile.fullprofile.me");
        }
        String email = getAttribute(attributes, BasicProfileAttributesSet.EMAIL, BasicProfileAttributesSet.IDENTIFIER,
                "profile", "profile.fullprofile.me");
        if (!StringUtils.hasText(email)) {
            // fall back to openid profile
            email = getAttribute(attributes, OpenIdProfileAttributesSet.EMAIL, OpenIdProfileAttributesSet.IDENTIFIER,
                    "profile", "profile.fullprofile.me");
        }

        profile.setName(name);
        profile.setSurname(surname);
        profile.setEmail(email);

        return profile;
    }

}
