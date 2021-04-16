package it.smartcommunitylab.aac.profiles.scope;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.profiles.model.ProfileClaimsSet;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.WhitelistScopeApprover;

@Component
public class BasicProfileScopeProvider extends ProfileScopeProvider {

    private static final Set<Scope> scopes;
    public static final Map<String, WhitelistScopeApprover> approvers;

    static {
        scopes = Collections.unmodifiableSet(Collections.singleton(new BasicProfileScope()));
        Map<String, WhitelistScopeApprover> a = new HashMap<>();
        for (Scope s : scopes) {
            a.put(s.getScope(), new WhitelistScopeApprover(null, s.getResourceId(), s.getScope()));
        }

        approvers = a;
    }

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID + ".basic";
    }

    @Override
    public Collection<Scope> getScopes() {
        return scopes;
    }

    @Override
    public ScopeApprover getApprover(String scope) {
        return approvers.get(scope);
    }

}
