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

import it.smartcommunitylab.aac.claims.ClaimsSet;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import it.smartcommunitylab.aac.users.model.User;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public abstract class ProfileClaimsExtractor implements ScopeClaimsExtractor {

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID;
    }

    public abstract String getKey();

    @Override
    public ClaimsSet extractUserClaims(
        String scope,
        User user,
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    ) throws InvalidDefinitionException, SystemException {
        AbstractProfile profile = buildUserProfile(user, scopes);

        // build a claimsSet
        ClaimsSet claimsSet = buildClaimsSet(scope, getKey(), profile, true);

        return claimsSet;
    }

    // subclasses need to provide the profile
    protected abstract AbstractProfile buildUserProfile(User user, Collection<String> scopes)
        throws InvalidDefinitionException;

    protected ClaimsSet buildClaimsSet(String scope, String key, AbstractProfile profile, boolean isUser) {
        ProfileClaimsSet claimsSet = new ProfileClaimsSet();
        claimsSet.setScope(scope);
        claimsSet.setKey(key);

        // by default profile claims are top level
        // if custom profiles enforce namespace as resourceId
        if (getResourceId().startsWith("aac.")) {
            claimsSet.setNamespace(null);
        } else {
            claimsSet.setNamespace(getResourceId());
        }

        // set profile
        claimsSet.setUser(isUser);
        claimsSet.setProfile(profile);

        return claimsSet;
    }

    @Override
    public ClaimsSet extractClientClaims(
        String scope,
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    ) throws InvalidDefinitionException, SystemException {
        // not supported now but subclasses can override
        return null;
    }

    @Override
    public String getRealm() {
        return null;
    }
}
