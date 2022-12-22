package it.smartcommunitylab.aac.api.scopes;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.approver.AuthorityScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractApiScopeProvider;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class AdminScopeProvider extends AbstractApiScopeProvider<AbstractInternalApiScope> {

    public AdminScopeProvider(AdminApiResource resource) {
        super(SystemKeys.AUTHORITY_INTERNAL, resource.getProvider(), resource.getScopes());
        Assert.notNull(resource, "resource can not be null");
    }

    @Override
    protected AuthorityScopeApprover<AbstractInternalApiScope> buildScopeApprover(AbstractInternalApiScope s) {
        // build approver
        AuthorityScopeApprover<AbstractInternalApiScope> sa = new AuthorityScopeApprover<>(s);
        // ask exact match on global authority
        Set<? extends GrantedAuthority> authorities = s.getAuthorities().stream()
                .map(a -> new SimpleGrantedAuthority(a)).collect(Collectors.toSet());
        sa.setGrantedAuthorities(authorities);

        return sa;
    }

}
