package it.smartcommunitylab.aac.roles.scopes;

import it.smartcommunitylab.aac.api.scopes.AbstractInternalApiResource;

public class RolesResource extends AbstractInternalApiResource {

    public static final String RESOURCE_ID = "aac.roles";

    public RolesResource(String realm) {
        super(realm, RESOURCE_ID);

        // statically register scopes
        setScopes(
                new ClientRolesScope(realm),
                new UserRolesScope(realm),
                new UserSpacesScope(realm));
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Roles";
    }

    @Override
    public String getDescription() {
        return "Access roles and authorities";
    }

}