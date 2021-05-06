package it.smartcommunitylab.aac.profiles.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.BasicProfileAttributesSet;
import it.smartcommunitylab.aac.profiles.OpenIdProfileAttributesSet;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;

@Component
public class OpenIdProfileExtractor extends UserProfileExtractor {

    @Override
    public OpenIdProfile extractUserProfile(User user)
            throws InvalidDefinitionException {

        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return null;
        }

        // TODO decide how to merge identities into a single profile
        // for now get first identity, should be last logged in
        UserIdentity id = identities.iterator().next();
        OpenIdProfile profile = extract(id.getAccount(), id.getAttributes());
        return profile;
    }

    @Override
    public OpenIdProfile extractUserProfile(UserIdentity identity) throws InvalidDefinitionException {
        if (identity == null) {
            return null;
        }

        return extract(identity.getAccount(), identity.getAttributes());
    }

    @Override
    public Collection<OpenIdProfile> extractUserProfiles(User user) throws InvalidDefinitionException {
        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return Collections.emptyList();
        }

        return identities.stream().map(id -> extract(id.getAccount(), id.getAttributes())).collect(Collectors.toList());
    }

    private OpenIdProfile extract(UserAccount account, Collection<UserAttributes> attributes) {
        OpenIdProfile profile = new OpenIdProfile();

        // username is not modifiable via attributes
        profile.setUsername(account.getUsername());

        // lookup attributes with default names in openid profile
        String givenName = getAttribute(attributes, OpenIdProfileAttributesSet.GIVEN_NAME,
                OpenIdProfileAttributesSet.IDENTIFIER,
                "profile", "profile.fullprofile.me");
        if (!StringUtils.hasText(givenName)) {
            // fall back to basic profile
            givenName = getAttribute(attributes, BasicProfileAttributesSet.NAME, BasicProfileAttributesSet.IDENTIFIER,
                    "profile", "profile.fullprofile.me");
        }
        String familyName = getAttribute(attributes, OpenIdProfileAttributesSet.FAMILY_NAME,
                OpenIdProfileAttributesSet.IDENTIFIER,
                "profile", "profile.fullprofile.me");
        if (!StringUtils.hasText(familyName)) {
            // fall back to basic profile
            familyName = getAttribute(attributes, BasicProfileAttributesSet.SURNAME,
                    BasicProfileAttributesSet.IDENTIFIER,
                    "profile", "profile.fullprofile.me");
        }
        String email = getAttribute(attributes, OpenIdProfileAttributesSet.EMAIL, OpenIdProfileAttributesSet.IDENTIFIER,
                "email",
                "profile", "profile.fullprofile.me");
        if (!StringUtils.hasText(email)) {
            // fall back to basic profile
            email = getAttribute(attributes, BasicProfileAttributesSet.EMAIL, BasicProfileAttributesSet.IDENTIFIER,
                    "profile", "profile.fullprofile.me");
        }

        profile.setGivenName(givenName);
        profile.setFamilyName(familyName);
        profile.setEmail(email);

        // lookup attributes with default names (oidc)

        profile.setMiddleName(
                getAttribute(attributes, OpenIdProfileAttributesSet.MIDDLE_NAME, OpenIdProfileAttributesSet.IDENTIFIER,
                        "profile", "profile.fullprofile.me"));
        profile.setNickName(
                getAttribute(attributes, OpenIdProfileAttributesSet.NICKNAME, OpenIdProfileAttributesSet.IDENTIFIER,
                        "profile", "profile.fullprofile.me"));
        profile.setPhone(
                getAttribute(attributes, OpenIdProfileAttributesSet.PHONE_NUMBER, OpenIdProfileAttributesSet.IDENTIFIER,
                        "phone", "profile", "profile.fullprofile.me"));
        profile.setProfileUrl(
                getAttribute(attributes, OpenIdProfileAttributesSet.PROFILE, OpenIdProfileAttributesSet.IDENTIFIER,
                        "profile", "profile.fullprofile.me"));
        profile.setPictureUrl(
                getAttribute(attributes, OpenIdProfileAttributesSet.PICTURE, OpenIdProfileAttributesSet.IDENTIFIER,
                        "profile", "profile.fullprofile.me"));
        profile.setWebsiteUrl(
                getAttribute(attributes, OpenIdProfileAttributesSet.WEBSITE, OpenIdProfileAttributesSet.IDENTIFIER,
                        "profile", "profile.fullprofile.me"));
        profile.setGender(
                getAttribute(attributes, OpenIdProfileAttributesSet.GENDER, OpenIdProfileAttributesSet.IDENTIFIER,
                        "profile", "profile.fullprofile.me"));

        String emailVerifiedAttr = getAttribute(attributes, OpenIdProfileAttributesSet.EMAIL_VERIFIED,
                OpenIdProfileAttributesSet.IDENTIFIER, "email",
                "profile", "profile.fullprofile.me");
        boolean emailVerified = emailVerifiedAttr != null ? Boolean.parseBoolean(emailVerifiedAttr) : false;
        profile.setEmailVerified(emailVerified);

        String phoneVerifiedAttr = getAttribute(attributes, OpenIdProfileAttributesSet.PHONE_NUMBER_VERIFIED,
                OpenIdProfileAttributesSet.IDENTIFIER, "phone",
                "profile", "profile.fullprofile.me");
        boolean phoneVerified = phoneVerifiedAttr != null ? Boolean.parseBoolean(phoneVerifiedAttr) : false;
        profile.setPhoneVerified(phoneVerified);

        try {
            String birthdateAttr = getAttribute(attributes, OpenIdProfileAttributesSet.BIRTHDATE,
                    OpenIdProfileAttributesSet.IDENTIFIER, "profile", "profile.fullprofile.me");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date birthdate = birthdateAttr != null ? formatter.parse(birthdateAttr) : null;
            profile.setBirthdate(birthdate);
        } catch (ParseException e) {
            profile.setBirthdate(null);
        }

        String zoneInfo = getAttribute(attributes, OpenIdProfileAttributesSet.ZONEINFO,
                OpenIdProfileAttributesSet.IDENTIFIER,
                "profile", "profile.fullprofile.me");
        if (!StringUtils.hasText(zoneInfo)) {
            zoneInfo = TimeZone.getDefault().getDisplayName();
        }
        profile.setZoneinfo(zoneInfo);

        String locale = getAttribute(attributes, OpenIdProfileAttributesSet.LOCALE,
                OpenIdProfileAttributesSet.IDENTIFIER,
                "profile", "profile.fullprofile.me");
        if (!StringUtils.hasText(locale)) {
            locale = Locale.getDefault().getDisplayName();
        }
        profile.setLocale(locale);

        return profile;
    }
}
