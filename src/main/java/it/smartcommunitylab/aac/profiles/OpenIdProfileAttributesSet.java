package it.smartcommunitylab.aac.profiles;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.attributes.model.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.model.DateAttribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.core.base.BaseAttributes;
import it.smartcommunitylab.aac.core.model.Attribute;

public class OpenIdProfileAttributesSet extends BaseAttributes {
    public static final String IDENTIFIER = "openid";

    private final String userId;
    private Map<String, Attribute> attributes;

    public OpenIdProfileAttributesSet(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, IDENTIFIER);
        Assert.hasText(userId, "userId can not be null or blank");
        this.userId = userId;
        this.attributes = new HashMap<>();
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getAttributesId() {
        return IDENTIFIER + ":" + userId;
    }

    @Override
    public Collection<String> getKeys() {
        return attributes.keySet();
    }

    @Override
    public Collection<Attribute> getAttributes() {
        return attributes.values();
    }

    public void setName(String name) {
        if (name == null) {
            attributes.remove(NAME);
            return;
        }

        StringAttribute attr = new StringAttribute(NAME);
        attr.setValue(name);

        attributes.put(NAME, attr);
    }

    public void setGivenName(String givenName) {
        if (givenName == null) {
            attributes.remove(GIVEN_NAME);
            return;
        }

        StringAttribute attr = new StringAttribute(GIVEN_NAME);
        attr.setValue(givenName);

        attributes.put(GIVEN_NAME, attr);
    }

    public void setFamilyName(String familyName) {
        if (familyName == null) {
            attributes.remove(FAMILY_NAME);
            return;
        }

        StringAttribute attr = new StringAttribute(FAMILY_NAME);
        attr.setValue(familyName);

        attributes.put(FAMILY_NAME, attr);
    }

    public void setMiddleName(String middleName) {
        if (middleName == null) {
            attributes.remove(MIDDLE_NAME);
            return;
        }

        StringAttribute attr = new StringAttribute(MIDDLE_NAME);
        attr.setValue(middleName);

        attributes.put(MIDDLE_NAME, attr);
    }

    public void setNickname(String nickname) {
        if (nickname == null) {
            attributes.remove(NICKNAME);
            return;
        }

        StringAttribute attr = new StringAttribute(NICKNAME);
        attr.setValue(nickname);

        attributes.put(NICKNAME, attr);
    }

    public void setPreferredUsername(String preferredUsername) {
        if (preferredUsername == null) {
            attributes.remove(PREFERRED_USERNAME);
            return;
        }

        StringAttribute attr = new StringAttribute(PREFERRED_USERNAME);
        attr.setValue(preferredUsername);

        attributes.put(PREFERRED_USERNAME, attr);
    }

    public void setEmail(String email) {
        if (email == null) {
            attributes.remove(EMAIL);
            return;
        }

        StringAttribute attr = new StringAttribute(EMAIL);
        attr.setValue(email);

        attributes.put(EMAIL, attr);
    }

    public void setEmailVerified(Boolean emailVerified) {
        if (emailVerified == null) {
            attributes.remove(EMAIL_VERIFIED);
            return;
        }

        BooleanAttribute attr = new BooleanAttribute(EMAIL_VERIFIED);
        attr.setValue(emailVerified);

        attributes.put(EMAIL_VERIFIED, attr);
    }

    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            attributes.remove(PHONE_NUMBER);
            return;
        }

        StringAttribute attr = new StringAttribute(PHONE_NUMBER);
        attr.setValue(phoneNumber);

        attributes.put(PHONE_NUMBER, attr);
    }

    public void setPhoneVerified(Boolean phoneVerified) {
        if (phoneVerified == null) {
            attributes.remove(PHONE_NUMBER_VERIFIED);
            return;
        }

        BooleanAttribute attr = new BooleanAttribute(PHONE_NUMBER_VERIFIED);
        attr.setValue(phoneVerified);

        attributes.put(PHONE_NUMBER_VERIFIED, attr);
    }

    public void setProfile(String profileUrl) {
        if (profileUrl == null) {
            attributes.remove(PROFILE);
            return;
        }

        StringAttribute attr = new StringAttribute(PROFILE);
        attr.setValue(profileUrl);

        attributes.put(PROFILE, attr);
    }

    public void setPicture(String pictureUrl) {
        if (pictureUrl == null) {
            attributes.remove(PICTURE);
            return;
        }

        StringAttribute attr = new StringAttribute(PICTURE);
        attr.setValue(pictureUrl);

        attributes.put(PICTURE, attr);
    }

    public void setWebsite(String websiteUrl) {
        if (websiteUrl == null) {
            attributes.remove(WEBSITE);
            return;
        }

        StringAttribute attr = new StringAttribute(WEBSITE);
        attr.setValue(websiteUrl);

        attributes.put(WEBSITE, attr);
    }

    public void setGender(String gender) {
        if (gender == null) {
            attributes.remove(GENDER);
            return;
        }

        StringAttribute attr = new StringAttribute(GENDER);
        attr.setValue(gender);

        attributes.put(GENDER, attr);
    }

    public void setBirthdate(Date birthdate) {
        if (birthdate == null) {
            attributes.remove(BIRTHDATE);
            return;
        }

        DateAttribute attr = new DateAttribute(BIRTHDATE);
        attr.setValue(birthdate);

        attributes.put(BIRTHDATE, attr);
    }

    public void setZoneinfo(String zoneinfo) {
        if (zoneinfo == null) {
            attributes.remove(ZONEINFO);
            return;
        }

        StringAttribute attr = new StringAttribute(ZONEINFO);
        attr.setValue(zoneinfo);

        attributes.put(ZONEINFO, attr);
    }

    public void setLocale(String locale) {
        if (locale == null) {
            attributes.remove(LOCALE);
            return;
        }

        StringAttribute attr = new StringAttribute(LOCALE);
        attr.setValue(locale);

        attributes.put(LOCALE, attr);
    }

    public static final String NAME = "name";
    public static final String GIVEN_NAME = "given_name";
    public static final String FAMILY_NAME = "family_name";
    public static final String MIDDLE_NAME = "middle_name";
    public static final String NICKNAME = "nickname";
    public static final String PREFERRED_USERNAME = "preferred_username";
    public static final String EMAIL = "email";
    public static final String EMAIL_VERIFIED = "email_verified";
    public static final String PHONE_NUMBER = "phone_number";
    public static final String PHONE_NUMBER_VERIFIED = "phone_number_verified";
    public static final String PROFILE = "profile";
    public static final String PICTURE = "picture";
    public static final String WEBSITE = "website";
    public static final String GENDER = "gender";
    public static final String BIRTHDATE = "birthdate";
    public static final String ZONEINFO = "zoneinfo";
    public static final String LOCALE = "locale";

}
