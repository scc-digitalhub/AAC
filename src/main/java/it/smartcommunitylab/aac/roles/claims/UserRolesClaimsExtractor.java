package it.smartcommunitylab.aac.roles.claims;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.User;

public class UserRolesClaimsExtractor implements ScopeClaimsExtractor {

    @Override
    public String getResourceId() {
        return "aac.roles";
    }

    @Override
    public Collection<String> getScopes() {
        return Collections.singleton(Config.SCOPE_USER_ROLE);
    }

    @Override
    public ClaimsSet extractUserClaims(String scope, User user, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions)
            throws InvalidDefinitionException, SystemException {

        // we get roles from user, it should be up-to-date
        Set<SpaceRole> spaceRoles = user.getSpaceRoles();

        // we also include realm roles
        Set<RealmRole> realmRoles = user.getRealmRoles();

        // fetch authorities
        // TODO evaluate dropping export of internal AAC authorities
        Set<RealmGrantedAuthority> authorities = user.getAuthorities().stream()
                .filter(r -> r instanceof RealmGrantedAuthority).map(r -> (RealmGrantedAuthority) r)
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
        claimsSet.setScope(Config.SCOPE_USER_ROLE);
        // we merge our map with namespace to tld
        claimsSet.setNamespace(null);
        claimsSet.setUser(true);
        claimsSet.setClaims(claims);

        return claimsSet;

    }

    @Override
    public ClaimsSet extractClientClaims(String scope, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions)
            throws InvalidDefinitionException, SystemException {
        // not supported
        return null;
    }

    @Override
    public String getRealm() {
        return null;
    }

}
