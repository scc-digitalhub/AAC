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

package it.smartcommunitylab.aac.groups.claims;

import it.smartcommunitylab.aac.claims.Claim;
import it.smartcommunitylab.aac.claims.ClaimsSet;
import it.smartcommunitylab.aac.claims.DefaultClaimsSet;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.claims.model.SerializableClaim;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.groups.model.Group;
import it.smartcommunitylab.aac.groups.scopes.ClientGroupsScope;
import it.smartcommunitylab.aac.groups.scopes.GroupsResource;
import it.smartcommunitylab.aac.users.model.User;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClientGroupsClaimsExtractor implements ScopeClaimsExtractor {

    @Override
    public String getResourceId() {
        return GroupsResource.RESOURCE_ID;
    }

    @Override
    public Collection<String> getScopes() {
        return Collections.singleton(ClientGroupsScope.SCOPE);
    }

    @Override
    public ClaimsSet extractUserClaims(
        String scope,
        User user,
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    ) throws InvalidDefinitionException, SystemException {
        // not supported
        return null;
    }

    @Override
    public ClaimsSet extractClientClaims(
        String scope,
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    ) throws InvalidDefinitionException, SystemException {
        // we get groups from client, it should be up-to-date
        Set<Group> groups = client.getGroups();

        // convert to a claims list by flattening
        List<Claim> claims = new ArrayList<>();

        SerializableClaim groupsClaim = new SerializableClaim("groups");
        List<String> groupClaims = groups.stream().map(r -> r.getGroup()).collect(Collectors.toList());
        groupsClaim.setValue(new ArrayList<>(groupClaims));
        claims.add(groupsClaim);

        // build a claimsSet
        DefaultClaimsSet claimsSet = new DefaultClaimsSet();
        claimsSet.setResourceId(getResourceId());
        claimsSet.setScope(ClientGroupsScope.SCOPE);
        // we merge our map with namespace to tld
        claimsSet.setNamespace(null);
        claimsSet.setClient(true);
        claimsSet.setClaims(claims);

        return claimsSet;
    }

    @Override
    public String getRealm() {
        return null;
    }
}
