package it.smartcommunitylab.aac.api.scopes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.scope.approver.AuthorityScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractScopeProvider;

public class AdminScopeProvider extends AbstractScopeProvider<AbstractInternalApiScope> {

    private final Map<String, AbstractInternalApiScope> scopes;
    private final Map<String, AuthorityScopeApprover<AbstractInternalApiScope>> approvers;

    public AdminScopeProvider(AdminApiResource resource) {
        super(SystemKeys.AUTHORITY_INTERNAL, resource.getProvider());
        Assert.notNull(resource, "resource can not be null");

        // extract scopes
        this.scopes = resource.getScopes().stream()
                .collect(Collectors.toMap(s -> s.getScope(), s -> s));

        // init approvers map
        approvers = new HashMap<>();
    }

    @Override
    public AbstractInternalApiScope findScope(String scope) {
        return scopes.get(scope);
    }

    @Override
    public AbstractInternalApiScope getScope(String scope) throws NoSuchScopeException {
        AbstractInternalApiScope s = findScope(scope);
        if (s == null) {
            throw new NoSuchScopeException();
        }

        return s;
    }

    @Override
    public Collection<AbstractInternalApiScope> listScopes() {
        return Collections.unmodifiableCollection(scopes.values());
    }

    @Override
    public AuthorityScopeApprover<AbstractInternalApiScope> getScopeApprover(String scope) throws NoSuchScopeException {
        AbstractInternalApiScope s = getScope(scope);
        if (!approvers.containsKey(scope)) {
            // build approver
            AuthorityScopeApprover<AbstractInternalApiScope> sa = new AuthorityScopeApprover<>(s);
            // ask exact match on global authority
            Set<? extends GrantedAuthority> authorities = s.getAuthorities().stream()
                    .map(a -> new SimpleGrantedAuthority(a)).collect(Collectors.toSet());
            sa.setGrantedAuthorities(authorities);
            approvers.put(scope, sa);
        }

        return approvers.get(scope);
    }

}
