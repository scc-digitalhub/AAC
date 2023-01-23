package it.smartcommunitylab.aac.groups.scopes;

import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.groups.GroupsResourceAuthority;
import it.smartcommunitylab.aac.groups.claims.GroupsClaim;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class GroupsResource extends
        AbstractInternalApiResource<it.smartcommunitylab.aac.groups.scopes.GroupsResource.AbstractGroupsScope, AbstractClaimDefinition> {

    public static final String RESOURCE_ID = "aac.groups";
    public static final String AUTHORITY = GroupsResourceAuthority.AUTHORITY;

    public GroupsResource(String realm, String baseUrl) {
        super(AUTHORITY, realm, baseUrl, RESOURCE_ID);

        // statically register scopes
        setScopes(
                new ClientGroupsScope(realm),
                new UserGroupsScope(realm));

        // register claim
        setClaims(GroupsClaim.DEFINITION);
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Groups";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Access groups for user and clients";
//    }

    public static class AbstractGroupsScope extends AbstractInternalApiScope {

        public AbstractGroupsScope(String realm, String scope) {
            super(AUTHORITY, realm, GroupsResource.RESOURCE_ID, scope);
        }

    }
}