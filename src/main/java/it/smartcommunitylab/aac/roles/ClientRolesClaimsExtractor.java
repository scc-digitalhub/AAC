package it.smartcommunitylab.aac.roles;

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
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.User;

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
    public ClaimsSet extractUserClaims(String scope, User user, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions)
            throws InvalidDefinitionException, SystemException {
        // not supported
        return null;
    }

    @Override
    public ClaimsSet extractClientClaims(String scope, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions)
            throws InvalidDefinitionException, SystemException {

        // we get roles from client, it should be up-to-date
        Set<SpaceRole> roles = client.getRoles();

        // we also include realm authorities, since we don't have a separate scope for
        // these
        Set<RealmGrantedAuthority> authorities = client.getAuthorities().stream()
                .filter(r -> r instanceof RealmGrantedAuthority).map(r -> (RealmGrantedAuthority) r)
                .collect(Collectors.toSet());

        // convert to a claims list flattening roles
        List<Claim> claims = new ArrayList<>();

        SerializableClaim roleClaim = new SerializableClaim("roles");
        List<String> rolesClaims = roles.stream().map(r -> r.getAuthority()).collect(Collectors.toList());
//        roleClaim.setValue(new ArrayList<>(rolesClaims));
//        claims.add(roleClaim);

        SerializableClaim authClaim = new SerializableClaim("authorities");
        List<String> authsClaims = authorities.stream().map(r -> r.getAuthority()).collect(Collectors.toList());
        authClaim.setValue(new ArrayList<>(authsClaims));
        claims.add(authClaim);

        // merge realm authorities in roles claims under realms/
        authsClaims.forEach(a -> {
            rolesClaims.add("realms/" + a);
        });
        roleClaim.setValue(new ArrayList<>(rolesClaims));
        claims.add(roleClaim);

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
