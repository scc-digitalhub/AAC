package it.smartcommunitylab.aac.groups.scopes;

import it.smartcommunitylab.aac.groups.claims.GroupsClaim;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;

public class GroupsResource extends AbstractInternalApiResource {

    public static final String RESOURCE_ID = "aac.groups";

    public GroupsResource(String realm, String baseUrl) {
        super(realm, baseUrl, RESOURCE_ID);

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

}