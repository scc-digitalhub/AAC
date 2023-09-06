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

package it.smartcommunitylab.aac.profiles.claims;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.extractor.UserProfileExtractor;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import java.util.Collection;
import java.util.Collections;
import org.springframework.util.Assert;

public class CustomProfileClaimsExtractor extends ProfileClaimsExtractor {

    private final UserProfileExtractor extractor;
    private final String identifier;

    public CustomProfileClaimsExtractor(UserProfileExtractor extractor) {
        Assert.notNull(extractor, "extractor can not be null");
        Assert.hasText(extractor.getIdentifier(), "identifier can not be null");
        this.extractor = extractor;
        this.identifier = extractor.getIdentifier();
    }

    private String getScope() {
        return "profile." + identifier + ".me";
    }

    @Override
    public Collection<String> getScopes() {
        return Collections.singleton(getScope());
    }

    @Override
    public String getKey() {
        // use key for custom profiles, enforce namespace
        return identifier;
    }

    @Override
    protected AbstractProfile buildUserProfile(User user, Collection<String> scopes) throws InvalidDefinitionException {
        AbstractProfile profile = extractor.extractUserProfile(user);
        return profile;
    }
}
