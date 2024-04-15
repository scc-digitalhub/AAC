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
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.roles.model.SpaceRole;
import it.smartcommunitylab.aac.users.model.User;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClientRolesClaimsExtractor implements ScopeClaimsExtractor {

    @Override
    public String getResourceId() {
        return "aac.roles";
    }

    @Override
    public Collection<String> getScopes() {
        return Collections.singleton(Config.SCOPE_CLIENT_ROLE);
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
        // we get roles from client, it should be up-to-date
        Set<SpaceRole> spaceRoles = client.getSpaceRoles();

        // we also include realm roles
        Set<RealmRole> realmRoles = client.getRealmRoles();

        // fetch authorities
        // TODO evaluate dropping export of internal AAC authorities
        Set<RealmGrantedAuthority> authorities = client
            .getAuthorities()
            .stream()
            .filter(r -> r instanceof RealmGrantedAuthority)
            .map(r -> (RealmGrantedAuthority) r)
            .collect(Collectors.toSet());

        // convert to a claims list flattening roles
        List<Claim> claims = new ArrayList<>();

        SerializableClaim realmRolesClaim = new SerializableClaim("roles");
        List<String> realmRolesClaims = realmRoles.stream().map(r -> r.getAuthority()).collect(Collectors.toList());
        realmRolesClaim.setValue(new ArrayList<>(realmRolesClaims));
        claims.add(realmRolesClaim);

        SerializableClaim authClaim = new SerializableClaim("authorities");
        List<String> authsClaims = authorities.stream().map(r -> r.getAuthority()).collect(Collectors.toList());
        authClaim.setValue(new ArrayList<>(authsClaims));
        claims.add(authClaim);

        SerializableClaim spaceRolesClaim = new SerializableClaim("spaceRoles");
        List<String> spaceRolesClaims = spaceRoles.stream().map(r -> r.getAuthority()).collect(Collectors.toList());
        // merge realm roles in space roles claims under realms/
        realmRolesClaims.forEach(a -> {
            spaceRolesClaims.add("realms/" + a);
        });
        spaceRolesClaim.setValue(new ArrayList<>(spaceRolesClaims));
        claims.add(spaceRolesClaim);

        // build a claimsSet
        DefaultClaimsSet claimsSet = new DefaultClaimsSet();
        claimsSet.setResourceId(getResourceId());
        claimsSet.setScope(Config.SCOPE_CLIENT_ROLE);
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
