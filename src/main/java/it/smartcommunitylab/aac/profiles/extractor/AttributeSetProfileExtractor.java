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

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.CustomProfile;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.util.Assert;

public class AttributeSetProfileExtractor extends AbstractUserProfileExtractor {

    // profile identifier, same as attribute set id
    private final String identifier;

    public AttributeSetProfileExtractor(String identifier) {
        Assert.hasText(identifier, "identifier can not be null or empty");
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public CustomProfile extractUserProfile(UserIdentity identity) throws InvalidDefinitionException {
        if (identity == null) {
            return null;
        }

        return extract(identity.getAttributes());
    }

    @Override
    public CustomProfile extractUserProfile(User user) throws InvalidDefinitionException {
        // fetch custom attributes
        List<UserAttributes> userAttributes = user
            .getAttributes()
            .stream()
            .filter(ua -> !ua.getIdentifier().startsWith("aac."))
            .collect(Collectors.toList());

        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return extract(userAttributes);
        }

        // TODO decide how to merge identities into a single profile
        // for now get first identity, should be last logged in
        UserIdentity id = identities.iterator().next();

        CustomProfile profile = extract(mergeAttributes(userAttributes, id.getAttributes()));
        return profile;
    }

    @Override
    public Collection<? extends CustomProfile> extractUserProfiles(User user) throws InvalidDefinitionException {
        // fetch custom attributes
        List<UserAttributes> userAttributes = user
            .getAttributes()
            .stream()
            .filter(ua -> !ua.getIdentifier().startsWith("aac."))
            .collect(Collectors.toList());

        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return Collections.singleton(extract(userAttributes));
        }

        return identities
            .stream()
            .map(id -> extract(mergeAttributes(userAttributes, id.getAttributes())))
            .collect(Collectors.toList());
    }

    private Collection<UserAttributes> mergeAttributes(
        Collection<UserAttributes> userAttributes,
        Collection<UserAttributes> identityAttributes
    ) {
        Map<String, UserAttributes> attributesMap = new HashMap<>();
        userAttributes.forEach(ua -> attributesMap.put(ua.getIdentifier(), ua));

        // merge attributes to user, override if present
        // TODO evaluate which attributes have precedence
        identityAttributes.forEach(ua -> attributesMap.put(ua.getIdentifier(), ua));

        return attributesMap.values();
    }

    private CustomProfile extract(Collection<UserAttributes> attributes) {
        CustomProfile profile = new CustomProfile(identifier);
        Optional<UserAttributes> attrs = attributes
            .stream()
            .filter(a -> identifier.equals(a.getIdentifier()))
            .findFirst();
        if (attrs.isPresent()) {
            attrs
                .get()
                .getAttributes()
                .forEach(a -> {
                    profile.addAttribute(a.getKey(), a.exportValue());
                });
        }

        return profile;
    }
}
