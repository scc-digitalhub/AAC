package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.ScopeProvider;
import it.smartcommunitylab.aac.scope.WhitelistScopeApprover;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/*
 * A simple scope provider which return profile scopes
 */
@Component
public class OpenIdProfileScopeProvider implements ScopeProvider {

    private static final OpenIdResource resource = new OpenIdResource();
    public static final Set<Scope> scopes;
    private static final Map<String, WhitelistScopeApprover> approvers;

    static {
        Set<Scope> s = new HashSet<>();
        s.add(new OpenIdDefaultScope());
        s.add(new OpenIdEmailScope());
        s.add(new OpenIdAddressScope());
        s.add(new OpenIdPhoneScope());

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
        return resource.getResourceId();
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
        if (approvers.containsKey(scope)) {
            return approvers.get(scope);
        }

        return null;
    }
}
