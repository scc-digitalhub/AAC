package it.smartcommunitylab.aac.roles.scopes;

import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.roles.RolesResourceAuthority;
import it.smartcommunitylab.aac.roles.claims.RolesClaim;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class RolesResource extends
        AbstractInternalApiResource<it.smartcommunitylab.aac.roles.scopes.RolesResource.AbstractRolesScope, AbstractClaimDefinition> {

    public static final String RESOURCE_ID = "aac.roles";
    public static final String AUTHORITY = RolesResourceAuthority.AUTHORITY;

    public RolesResource(String realm, String baseUrl) {
        super(AUTHORITY, realm, baseUrl, RESOURCE_ID);

        // statically register scopes
        setScopes(
                new UserSpacesScope(realm),
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

    public static class AbstractRolesScope extends AbstractInternalApiScope {

        public AbstractRolesScope(String realm, String scope) {
            super(AUTHORITY, realm, RESOURCE_ID, scope);
        }

    }
}