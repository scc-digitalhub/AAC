package it.smartcommunitylab.aac.roles;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.ScopeProvider;
import it.smartcommunitylab.aac.scope.WhitelistScopeApprover;

@Component
public class RolesScopeProvider implements ScopeProvider {

    private static final RolesResource resource = new RolesResource();
    public static final Map<String, WhitelistScopeApprover> approvers;

    static {
        Map<String, WhitelistScopeApprover> a = new HashMap<>();
        for (Scope s : resource.getScopes()) {
            a.put(s.getScope(), new WhitelistScopeApprover(null, s.getResourceId(), s.getScope()));
        }

        approvers = a;
    }

    @Override
    public String getResourceId() {
        return RolesResource.RESOURCE_ID;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public Collection<Scope> getScopes() {
        return resource.getScopes();
    }

    @Override
    public ScopeApprover getApprover(String scope) {
        return approvers.get(scope);
    }

}
