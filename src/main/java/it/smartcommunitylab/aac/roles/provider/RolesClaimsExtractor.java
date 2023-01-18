package it.smartcommunitylab.aac.roles.provider;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.claims.base.AbstractClaim;
import it.smartcommunitylab.aac.claims.base.AbstractResourceClaimsExtractor;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.roles.claims.RolesClaim;
import it.smartcommunitylab.aac.roles.model.RealmRole;
import it.smartcommunitylab.aac.roles.scopes.ClientRolesScope;
import it.smartcommunitylab.aac.roles.scopes.RolesResource;
import it.smartcommunitylab.aac.roles.scopes.UserRolesScope;

public class RolesClaimsExtractor extends AbstractResourceClaimsExtractor<RolesResource> {

    public RolesClaimsExtractor(RolesResource resource) {
        super(resource);
    }

    @Override
    protected List<AbstractClaim> extractUserClaims(User user, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {

        // check if scope is present
        if (scopes == null || !scopes.contains(UserRolesScope.SCOPE)) {
            return null;
        }

        // we get roles from user, it should be up-to-date
        Set<RealmRole> roles = user.getRealmRoles();

        // build a claim for every role
        // note: we filter roles on client realm
        List<AbstractClaim> claims = roles.stream()
                .filter(r -> r.getRealm().equals(client.getRealm()))
                .map(r -> new RolesClaim(r.getRole()))
                .collect(Collectors.toList());

        return claims;
    }

    @Override
    protected List<AbstractClaim> extractClientClaims(ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {

        // check if scope is present
        if (scopes == null || !scopes.contains(ClientRolesScope.SCOPE)) {
            return null;
        }

        // we get roles from client, it should be up-to-date
        Set<RealmRole> roles = client.getRealmRoles();

        // build a claim for every group
        List<AbstractClaim> claims = roles.stream()
                .map(r -> new RolesClaim(r.getRole()))
                .collect(Collectors.toList());

        return claims;
    }

}
