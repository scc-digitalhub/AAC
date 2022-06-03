package it.smartcommunitylab.aac.profiles.extractor;

import java.util.Collection;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.EmailProfile;

public class EmailProfileExtractor implements UserProfileExtractor {

    @Override
    public String getIdentifier() {
        return EmailProfile.IDENTIFIER;
    }

    @Override
    public EmailProfile extractUserProfile(User user)
            throws InvalidDefinitionException {

        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return null;
        }

        // get first identity as base, should be last logged in
        UserIdentity id = identities.iterator().next();

        // build profile from user regardless of identities
        EmailProfile profile = new EmailProfile(id.getAuthority(), id.getProvider(), user.getRealm(),
                user.getUserId());
        profile.setEmail(user.getEmail());
        profile.setEmailVerified(user.isEmailVerified());

        return profile;
    }

//    private EmailProfile extract(UserAccount account, Collection<UserAttributes> attributes) {
//        EmailProfile profile = new EmailProfile(account.getAuthority(), account.getProvider(), account.getRealm(),
//                account.getUserId());
//
//        String email = getStringAttribute(
//                getAttribute(attributes, EmailAttributesSet.EMAIL, EmailAttributesSet.IDENTIFIER,
//                        BasicAttributesSet.IDENTIFIER, OpenIdAttributesSet.IDENTIFIER,
//                        "profile"));
//        profile.setEmail(email);
//
//        Boolean emailVerified = getBooleanAttribute(
//                getAttribute(attributes, EmailAttributesSet.EMAIL_VERIFIED, EmailAttributesSet.IDENTIFIER,
//                        OpenIdAttributesSet.IDENTIFIER, "profile"));
//        profile.setEmailVerified(emailVerified);
//
//        return profile;
//    }
}