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

import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.EmailAttributesSet;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public class BasicProfileExtractor extends AbstractUserProfileExtractor {

    @Override
    public String getIdentifier() {
        return BasicProfile.IDENTIFIER;
    }

    @Override
    public BasicProfile extractUserProfile(User user) throws InvalidDefinitionException {
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
            getAttribute(attributes, BasicAttributesSet.NAME, BasicAttributesSet.IDENTIFIER, "profile")
        );
        if (!StringUtils.hasText(name)) {
            // fall back to openid profile
            name =
                getStringAttribute(
                    getAttribute(attributes, OpenIdAttributesSet.GIVEN_NAME, OpenIdAttributesSet.IDENTIFIER, "profile")
                );
        }
        String surname = getStringAttribute(
            getAttribute(attributes, BasicAttributesSet.SURNAME, BasicAttributesSet.IDENTIFIER, "profile")
        );
        if (!StringUtils.hasText(surname)) {
            // fall back to openid profile
            surname =
                getStringAttribute(
                    getAttribute(attributes, OpenIdAttributesSet.FAMILY_NAME, OpenIdAttributesSet.IDENTIFIER, "profile")
                );
        }
        String email = getStringAttribute(
            getAttribute(
                attributes,
                BasicAttributesSet.EMAIL,
                BasicAttributesSet.IDENTIFIER,
                EmailAttributesSet.IDENTIFIER,
                OpenIdAttributesSet.IDENTIFIER,
                "profile"
            )
        );

        profile.setName(name);
        profile.setSurname(surname);
        profile.setEmail(email);

        return profile;
    }
}
