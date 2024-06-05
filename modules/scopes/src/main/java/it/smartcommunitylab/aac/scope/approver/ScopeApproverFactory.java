package it.smartcommunitylab.aac.scope.approver;

import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.model.Scope;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class ScopeApproverFactory {

    public <S extends Scope> WhitelistScopeApprover.Builder<S> whitelist(S s) {
        return new WhitelistScopeApprover.Builder<S>(s);
    }

    public <S extends Scope> AuthorityScopeApprover<S> from(Set<? extends GrantedAuthority> grantedAuthorities) {
        return null;
    }

    public <S extends Scope> ScopeApprover<?> policy(String policy) {
        // an expression defining a tree-like representation of approvers
        // combination is supported only via anyOf/allOf
        // example: allOf(authority(any:ROLE_ADMIN, ROLE_DEVELOPER), subjectType(user)

        return null;
    }
}
