package it.smartcommunitylab.aac.profiles.extractor;

import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.EmailAttributesSet;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.EmailProfile;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class EmailProfileExtractor extends AbstractUserProfileExtractor {

    @Override
    public String getIdentifier() {
        return EmailProfile.IDENTIFIER;
    }

    @Override
    public EmailProfile extractUserProfile(User user) throws InvalidDefinitionException {
        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return null;
        }

        // TODO decide how to merge identities into a single profile
        // for now get first identity, should be last logged in
        UserIdentity id = identities.iterator().next();
        EmailProfile profile = extract(id.getAccount(), id.getAttributes());
        return profile;
    }

    @Override
    public EmailProfile extractUserProfile(UserIdentity identity) throws InvalidDefinitionException {
        if (identity == null) {
            return null;
        }

        return extract(identity.getAccount(), identity.getAttributes());
    }

    @Override
    public Collection<EmailProfile> extractUserProfiles(User user) throws InvalidDefinitionException {
        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return Collections.emptyList();
        }

        return identities.stream().map(id -> extract(id.getAccount(), id.getAttributes())).collect(Collectors.toList());
    }

    private EmailProfile extract(UserAccount account, Collection<UserAttributes> attributes) {
        EmailProfile profile = new EmailProfile();

        String email = getStringAttribute(
            getAttribute(
                attributes,
                EmailAttributesSet.EMAIL,
                EmailAttributesSet.IDENTIFIER,
                BasicAttributesSet.IDENTIFIER,
                OpenIdAttributesSet.IDENTIFIER,
                "profile"
            )
        );
        profile.setEmail(email);

        Boolean emailVerified = getBooleanAttribute(
            getAttribute(
                attributes,
                EmailAttributesSet.EMAIL_VERIFIED,
                EmailAttributesSet.IDENTIFIER,
                OpenIdAttributesSet.IDENTIFIER,
                "profile"
            )
        );
        profile.setEmailVerified(emailVerified);

        return profile;
    }
}
