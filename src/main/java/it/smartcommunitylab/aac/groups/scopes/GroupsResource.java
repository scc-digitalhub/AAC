package it.smartcommunitylab.aac.groups.scopes;

import it.smartcommunitylab.aac.api.scopes.AbstractInternalApiResource;

public class GroupsResource extends AbstractInternalApiResource {

    public static final String RESOURCE_ID = "aac.groups";

    public GroupsResource(String realm) {
        super(realm, RESOURCE_ID);

        // statically register scopes
        setScopes(
                new ClientGroupsScope(realm),
                new UserGroupsScope(realm));
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Groups";
    }

    @Override
    public String getDescription() {
        return "Access groups for user and clients";
    }

}