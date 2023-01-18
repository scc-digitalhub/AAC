package it.smartcommunitylab.aac.api.provider;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.scope.approver.AuthorityScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractScopeProvider;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class AACApiScopeProvider extends AbstractScopeProvider<AbstractInternalApiScope> {

    public AACApiScopeProvider(AbstractInternalApiScope scope) {
        super(SystemKeys.AUTHORITY_INTERNAL, scope.getProvider(), scope.getRealm(), scope);

        // build approver
        AuthorityScopeApprover<AbstractInternalApiScope> sa = new AuthorityScopeApprover<>(scope);
        // ask exact match on realm authority
        Set<? extends GrantedAuthority> authorities = scope.getAuthorities().stream()
                .map(a -> new RealmGrantedAuthority(scope.getRealm(), a)).collect(Collectors.toSet());
        sa.setGrantedAuthorities(authorities);

        // set custom approver
        this.approver = sa;
    }

}
