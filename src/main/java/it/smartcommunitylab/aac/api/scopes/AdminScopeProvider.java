package it.smartcommunitylab.aac.api.scopes;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.approver.AuthorityScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractScopeProvider;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class AdminScopeProvider extends AbstractScopeProvider<AbstractInternalApiScope> {

    public AdminScopeProvider(AbstractInternalApiScope scope) {
        super(SystemKeys.AUTHORITY_INTERNAL, scope.getProvider(), scope.getRealm(), scope);

        // build approver
        AuthorityScopeApprover<AbstractInternalApiScope> sa = new AuthorityScopeApprover<>(scope);
        // ask exact match on global authority
        Set<? extends GrantedAuthority> authorities = scope.getAuthorities().stream()
                .map(a -> new SimpleGrantedAuthority(a)).collect(Collectors.toSet());
        sa.setGrantedAuthorities(authorities);

        //set custom approver
        setApprover(sa);
    }

}
