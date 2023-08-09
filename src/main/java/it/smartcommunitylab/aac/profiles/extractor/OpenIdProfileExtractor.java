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

package it.smartcommunitylab.aac.profiles.extractor;

import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.EmailAttributesSet;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.identity.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenIdProfileExtractor extends AbstractUserProfileExtractor {

    @Override
    public String getIdentifier() {
        return OpenIdProfile.IDENTIFIER;
    }

    @Override
    public OpenIdProfile extractUserProfile(User user) throws InvalidDefinitionException {
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
        String givenName = getStringAttribute(
            getAttribute(attributes, OpenIdAttributesSet.GIVEN_NAME, OpenIdAttributesSet.IDENTIFIER, "profile")
        );
        if (!StringUtils.hasText(givenName)) {
            // fall back to basic profile
            givenName =
                getStringAttribute(
                    getAttribute(attributes, BasicAttributesSet.NAME, BasicAttributesSet.IDENTIFIER, "profile")
                );
        }
        String familyName = getStringAttribute(
            getAttribute(attributes, OpenIdAttributesSet.FAMILY_NAME, OpenIdAttributesSet.IDENTIFIER, "profile")
        );
        if (!StringUtils.hasText(familyName)) {
            // fall back to basic profile
            familyName =
                getStringAttribute(
                    getAttribute(attributes, BasicAttributesSet.SURNAME, BasicAttributesSet.IDENTIFIER, "profile")
                );
        }
        String email = getStringAttribute(
            getAttribute(
                attributes,
                OpenIdAttributesSet.EMAIL,
                OpenIdAttributesSet.IDENTIFIER,
                EmailAttributesSet.IDENTIFIER,
                BasicAttributesSet.IDENTIFIER,
                "profile"
            )
        );

        profile.setGivenName(givenName);
        profile.setFamilyName(familyName);
        profile.setEmail(email);

        // lookup attributes with default names (oidc)

        profile.setMiddleName(
            getStringAttribute(
                getAttribute(attributes, OpenIdAttributesSet.MIDDLE_NAME, OpenIdAttributesSet.IDENTIFIER, "profile")
            )
        );
        profile.setNickName(
            getStringAttribute(
                getAttribute(attributes, OpenIdAttributesSet.NICKNAME, OpenIdAttributesSet.IDENTIFIER, "profile")
            )
        );
        profile.setPhone(
            getStringAttribute(
                getAttribute(
                    attributes,
                    OpenIdAttributesSet.PHONE_NUMBER,
                    OpenIdAttributesSet.IDENTIFIER,
                    "phone",
                    "profile"
                )
            )
        );
        profile.setProfileUrl(
            getStringAttribute(
                getAttribute(attributes, OpenIdAttributesSet.PROFILE, OpenIdAttributesSet.IDENTIFIER, "profile")
            )
        );
        profile.setPictureUrl(
            getStringAttribute(
                getAttribute(attributes, OpenIdAttributesSet.PICTURE, OpenIdAttributesSet.IDENTIFIER, "profile")
            )
        );
        profile.setWebsiteUrl(
            getStringAttribute(
                getAttribute(attributes, OpenIdAttributesSet.WEBSITE, OpenIdAttributesSet.IDENTIFIER, "profile")
            )
        );
        profile.setGender(
            getStringAttribute(
                getAttribute(attributes, OpenIdAttributesSet.GENDER, OpenIdAttributesSet.IDENTIFIER, "profile")
            )
        );

        Boolean emailVerified = getBooleanAttribute(
            getAttribute(
                attributes,
                OpenIdAttributesSet.EMAIL_VERIFIED,
                OpenIdAttributesSet.IDENTIFIER,
                "email",
                "profile"
            )
        );
        profile.setEmailVerified(emailVerified);

        Boolean phoneVerified = getBooleanAttribute(
            getAttribute(
                attributes,
                OpenIdAttributesSet.PHONE_NUMBER_VERIFIED,
                OpenIdAttributesSet.IDENTIFIER,
                "phone",
                "profile"
            )
        );
        profile.setPhoneVerified(phoneVerified);

        LocalDate birthdate = getDateAttribute(
            getAttribute(attributes, OpenIdAttributesSet.BIRTHDATE, OpenIdAttributesSet.IDENTIFIER, "profile")
        );
        profile.setBirthdate(birthdate);

        String zoneInfo = getStringAttribute(
            getAttribute(attributes, OpenIdAttributesSet.ZONEINFO, OpenIdAttributesSet.IDENTIFIER, "profile")
        );
        if (!StringUtils.hasText(zoneInfo)) {
            zoneInfo = ZonedDateTime.now().getZone().getId();
        }
        profile.setZoneinfo(zoneInfo);

        String locale = getStringAttribute(
            getAttribute(attributes, OpenIdAttributesSet.LOCALE, OpenIdAttributesSet.IDENTIFIER, "profile")
        );
        if (!StringUtils.hasText(locale)) {
            locale = Locale.getDefault().toLanguageTag();
        }
        profile.setLocale(locale);

        return profile;
    }
}
