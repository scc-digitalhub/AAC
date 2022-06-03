package it.smartcommunitylab.aac.profiles.extractor;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;

@Component
public class OpenIdProfileExtractor extends AbstractUserProfileExtractor {

    @Override
    public String getIdentifier() {
        return OpenIdProfile.IDENTIFIER;
    }

    @Override
    public OpenIdProfile extractUserProfile(User user)
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

        OpenIdProfile profile = extract(id.getAccount(), attributes);
        return profile;
    }

    public OpenIdProfile extractUserProfile(UserIdentity identity) throws InvalidDefinitionException {
        if (identity == null) {
            return null;
        }

        return extract(identity.getAccount(), identity.getAttributes());
    }

    private OpenIdProfile extract(UserAccount account, Collection<UserAttributes> attributes) {
        OpenIdProfile profile = new OpenIdProfile(account.getAuthority(), account.getProvider(), account.getRealm(),
                account.getUserId());

        // username is not modifiable via attributes
        profile.setUsername(account.getUsername());

        // email is not modifiable via attributes
        profile.setEmail(account.getEmailAddress());

        // lookup attributes with default names in openid profile
        String givenName = getStringAttribute(getAttribute(attributes, OpenIdAttributesSet.GIVEN_NAME,
                OpenIdAttributesSet.IDENTIFIER,
                "profile"));
        if (!StringUtils.hasText(givenName)) {
            // fall back to basic profile
            givenName = getStringAttribute(
                    getAttribute(attributes, BasicAttributesSet.NAME, BasicAttributesSet.IDENTIFIER,
                            "profile"));
        }
        String familyName = getStringAttribute(getAttribute(attributes, OpenIdAttributesSet.FAMILY_NAME,
                OpenIdAttributesSet.IDENTIFIER,
                "profile"));
        if (!StringUtils.hasText(familyName)) {
            // fall back to basic profile
            familyName = getStringAttribute(getAttribute(attributes, BasicAttributesSet.SURNAME,
                    BasicAttributesSet.IDENTIFIER,
                    "profile"));
        }

        profile.setGivenName(givenName);
        profile.setFamilyName(familyName);

        // lookup attributes with default names (oidc)
        profile.setMiddleName(getStringAttribute(
                getAttribute(attributes, OpenIdAttributesSet.MIDDLE_NAME, OpenIdAttributesSet.IDENTIFIER,
                        "profile")));
        profile.setNickName(getStringAttribute(
                getAttribute(attributes, OpenIdAttributesSet.NICKNAME, OpenIdAttributesSet.IDENTIFIER,
                        "profile")));
        profile.setPhone(getStringAttribute(
                getAttribute(attributes, OpenIdAttributesSet.PHONE_NUMBER, OpenIdAttributesSet.IDENTIFIER,
                        "phone", "profile")));
        profile.setProfileUrl(getStringAttribute(
                getAttribute(attributes, OpenIdAttributesSet.PROFILE, OpenIdAttributesSet.IDENTIFIER,
                        "profile")));
        profile.setPictureUrl(getStringAttribute(
                getAttribute(attributes, OpenIdAttributesSet.PICTURE, OpenIdAttributesSet.IDENTIFIER,
                        "profile")));
        profile.setWebsiteUrl(getStringAttribute(
                getAttribute(attributes, OpenIdAttributesSet.WEBSITE, OpenIdAttributesSet.IDENTIFIER,
                        "profile")));
        profile.setGender(getStringAttribute(
                getAttribute(attributes, OpenIdAttributesSet.GENDER, OpenIdAttributesSet.IDENTIFIER,
                        "profile")));

        Boolean emailVerified = getBooleanAttribute(
                getAttribute(attributes, OpenIdAttributesSet.EMAIL_VERIFIED,
                        OpenIdAttributesSet.IDENTIFIER, "email", "profile"));
        profile.setEmailVerified(emailVerified);

        Boolean phoneVerified = getBooleanAttribute(
                getAttribute(attributes, OpenIdAttributesSet.PHONE_NUMBER_VERIFIED,
                        OpenIdAttributesSet.IDENTIFIER, "phone",
                        "profile"));
        profile.setPhoneVerified(phoneVerified);

        LocalDate birthdate = getDateAttribute(getAttribute(attributes, OpenIdAttributesSet.BIRTHDATE,
                OpenIdAttributesSet.IDENTIFIER, "profile"));
        profile.setBirthdate(birthdate);

        String zoneInfo = getStringAttribute(getAttribute(attributes, OpenIdAttributesSet.ZONEINFO,
                OpenIdAttributesSet.IDENTIFIER, "profile"));
        if (!StringUtils.hasText(zoneInfo)) {
            zoneInfo = ZonedDateTime.now().getZone().getId();
        }
        profile.setZoneinfo(zoneInfo);

        String locale = getStringAttribute(getAttribute(attributes, OpenIdAttributesSet.LOCALE,
                OpenIdAttributesSet.IDENTIFIER,
                "profile"));
        if (!StringUtils.hasText(locale)) {
            locale = Locale.getDefault().toLanguageTag();
        }
        profile.setLocale(locale);

        return profile;
    }
}
