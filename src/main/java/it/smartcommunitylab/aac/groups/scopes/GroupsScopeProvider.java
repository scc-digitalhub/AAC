package it.smartcommunitylab.aac.groups.scopes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.ScopeProvider;
import it.smartcommunitylab.aac.scope.WhitelistScopeApprover;

@Component
public class GroupsScopeProvider implements ScopeProvider {

    private static final GroupsResource resource = new GroupsResource();
    private static final Set<Scope> scopes;
    public static final Map<String, WhitelistScopeApprover> approvers;

    static {
        Set<Scope> s = new HashSet<>();
        s.add(new UserGroupsScope());
        s.add(new ClientGroupsScope());

        scopes = Collections.unmodifiableSet(s);
        resource.setScopes(scopes);

        Map<String, WhitelistScopeApprover> a = new HashMap<>();
        for (Scope sc : scopes) {
            a.put(sc.getScope(), new WhitelistScopeApprover(null, sc.getResourceId(), sc.getScope()));
        }

        approvers = a;
    }

    @Override
    public String getResourceId() {
        return GroupsResource.RESOURCE_ID;
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
