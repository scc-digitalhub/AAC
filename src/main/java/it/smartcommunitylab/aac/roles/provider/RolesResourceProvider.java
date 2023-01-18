package it.smartcommunitylab.aac.roles.provider;

import it.smartcommunitylab.aac.roles.scopes.RolesResource;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractInternalResourceProvider;

public class RolesResourceProvider extends AbstractInternalResourceProvider<RolesResource> {

    protected RolesResourceProvider(RolesResource resource) {
        super(resource);
    }

    @Override
    protected RolesScopeProvider buildScopeProvider(AbstractInternalApiScope scope) {
        return new RolesScopeProvider(scope);
    }

    @Override
    protected RolesClaimsExtractor buildClaimsExtractor(RolesResource resource) {
        return new RolesClaimsExtractor(resource);
    }

}