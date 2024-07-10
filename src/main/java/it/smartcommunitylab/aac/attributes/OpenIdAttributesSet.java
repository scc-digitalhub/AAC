/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.attributes;

import it.smartcommunitylab.aac.attributes.base.BaseAttributeSet;
import it.smartcommunitylab.aac.attributes.model.Attribute;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.attributes.model.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.model.DateAttribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenIdAttributesSet extends BaseAttributeSet {

    public static final String IDENTIFIER = "aac.openid";
    private static final List<String> keys;

    private Map<String, Attribute> attributes;

    public OpenIdAttributesSet() {
        this.attributes = new HashMap<>();
    }

    public OpenIdAttributesSet(Collection<Attribute> attrs) {
        this.attributes = new HashMap<>();
        if (attrs != null) {
            attrs.forEach(a -> this.attributes.put(a.getKey(), a));
        }
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Collection<String> getKeys() {
        return keys;
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

    public void setBirthdate(LocalDate birthdate) {
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

    @Override
    public String getName() {
        // TODO i18n
        return "OpenId user attribute set";
    }

    @Override
    public String getDescription() {
        return "OpenId Connect default user attribute set";
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

    static {
        List<String> k = new ArrayList<>();
        k.add(NAME);
        k.add(GIVEN_NAME);
        k.add(FAMILY_NAME);
        k.add(MIDDLE_NAME);
        k.add(NICKNAME);
        k.add(PREFERRED_USERNAME);
        k.add(EMAIL);
        k.add(EMAIL_VERIFIED);
        k.add(PHONE_NUMBER);
        k.add(PHONE_NUMBER_VERIFIED);
        k.add(PROFILE);
        k.add(PICTURE);
        k.add(WEBSITE);
        k.add(GENDER);
        k.add(BIRTHDATE);
        k.add(ZONEINFO);
        k.add(LOCALE);
        keys = Collections.unmodifiableList(k);
    }
}
