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

package it.smartcommunitylab.aac.roles.claims;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.claims.Claim;
import it.smartcommunitylab.aac.claims.ClaimsSet;
import it.smartcommunitylab.aac.claims.DefaultClaimsSet;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.claims.model.SerializableClaim;
import it.smartcommunitylab.aac.claims.model.StringClaim;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.users.model.User;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public class SpacesClaimsExtractor implements ScopeClaimsExtractor {

    public static final String SPACES_EXTENSIONS_KEY = "aac.roles.spaces_selection";

    @Override
    public String getResourceId() {
        return "aac.roles";
    }

    @Override
    public Collection<String> getScopes() {
        return Collections.singleton(Config.SCOPE_USER_SPACES);
    }

    @Override
    public ClaimsSet extractUserClaims(
        String scope,
        User user,
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    ) throws InvalidDefinitionException, SystemException {
        // we get roles from user, it should be up-to-date
        Set<SpaceRole> roles = user.getSpaceRoles();

        // filter context if specified by client
        if (StringUtils.hasText(client.getHookUniqueSpaces())) {
            roles =
                roles
                    .stream()
                    .filter(r -> r.getContext() != null && r.getContext().startsWith(client.getHookUniqueSpaces()))
                    .collect(Collectors.toSet());
        }

        // export as space name
        Set<String> spaces = roles.stream().map(r -> r.getSpace()).collect(Collectors.toSet());

        String space = null;
        // check if we get a selection in extensions
        if (extensions != null) {
            if (extensions.containsKey(SPACES_EXTENSIONS_KEY)) {
                try {
                    space = (String) extensions.get(SPACES_EXTENSIONS_KEY);
                } catch (Exception e) {
                    // ignore
                    space = null;
                }
            }
        }

        if (space != null && spaces.contains(space)) {
            spaces = Collections.singleton(space);
        }

        // convert to claim
        // if someone has performed a selection we will output a single space
        List<Claim> claims = new ArrayList<>();
        if (spaces.size() == 1) {
            StringClaim sc = new StringClaim("space");
            sc.setValue(spaces.iterator().next());
            claims.add(sc);
        } else {
            SerializableClaim sc = new SerializableClaim("space");
            sc.setValue(new ArrayList<>(spaces));
            claims.add(sc);
        }

        // build a claimsSet
        DefaultClaimsSet claimsSet = new DefaultClaimsSet();
        claimsSet.setResourceId(getResourceId());
        claimsSet.setScope(Config.SCOPE_USER_SPACES);
        // we merge our map with namespace to tld
        claimsSet.setNamespace(null);
        claimsSet.setUser(true);
        claimsSet.setClaims(claims);

        return claimsSet;
    }

    @Override
    public ClaimsSet extractClientClaims(
        String scope,
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    ) throws InvalidDefinitionException, SystemException {
        // not supported
        return null;
    }

    @Override
    public String getRealm() {
        return null;
    }
}
