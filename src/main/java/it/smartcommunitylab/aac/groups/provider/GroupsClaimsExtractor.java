package it.smartcommunitylab.aac.groups.provider;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.claims.base.AbstractClaim;
import it.smartcommunitylab.aac.claims.base.AbstractResourceClaimsExtractor;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.groups.claims.GroupsClaim;
import it.smartcommunitylab.aac.groups.model.Group;
import it.smartcommunitylab.aac.groups.scopes.ClientGroupsScope;
import it.smartcommunitylab.aac.groups.scopes.GroupsResource;
import it.smartcommunitylab.aac.groups.scopes.UserGroupsScope;
import it.smartcommunitylab.aac.model.User;

public class GroupsClaimsExtractor extends AbstractResourceClaimsExtractor<GroupsResource> {

    public GroupsClaimsExtractor(GroupsResource resource) {
        super(resource);
    }

    @Override
    protected List<AbstractClaim> extractUserClaims(User user, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {

        // check if scope is present
        if (scopes == null || !scopes.contains(UserGroupsScope.SCOPE)) {
            return null;
        }

        // we get groups from user, it should be up-to-date
        Set<Group> groups = user.getGroups();

        // build a claim for every group
        // note: we filter groups on client realm

        List<AbstractClaim> claims = groups.stream()
                .filter(g -> g.getRealm().equals(client.getRealm()))
                .map(g -> new GroupsClaim(g.getGroup()))
                .collect(Collectors.toList());

        return claims;
    }

    @Override
    protected List<AbstractClaim> extractClientClaims(ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {

        // check if scope is present
        if (scopes == null || !scopes.contains(ClientGroupsScope.SCOPE)) {
            return null;
        }

        // we get groups from client, it should be up-to-date
        Set<Group> groups = client.getGroups();

        // build a claim for every group
        List<AbstractClaim> claims = groups.stream()
                .map(g -> new GroupsClaim(g.getGroup()))
                .collect(Collectors.toList());

        return claims;
    }

}
