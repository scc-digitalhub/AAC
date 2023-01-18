package it.smartcommunitylab.aac.roles.scopes;

import it.smartcommunitylab.aac.roles.claims.RolesClaim;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;

public class RolesResource extends AbstractInternalApiResource {

    public static final String RESOURCE_ID = "aac.roles";

    public RolesResource(String realm, String baseUrl) {
        super(realm, baseUrl, RESOURCE_ID);

        // statically register scopes
        setScopes(
//                new UserSpacesScope(realm),
                new ClientRolesScope(realm),
                new UserRolesScope(realm));

        // register claim
        setClaims(RolesClaim.DEFINITION);
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Roles";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Access roles and authorities";
//    }

}