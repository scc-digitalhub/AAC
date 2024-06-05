package it.smartcommunitylab.aac.scope.provider;

import it.smartcommunitylab.aac.scope.approver.AuthorityScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractScopeProvider;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class AuthorityInternalScopeProvider<S extends AbstractInternalApiScope> extends AbstractScopeProvider<S> {

    public AuthorityInternalScopeProvider(S s) {
        this(s, true, false);
    }

    public AuthorityInternalScopeProvider(S s, boolean asRealm, boolean requireAll) {
        super(s.getAuthority(), s.getProvider(), s.getRealm(), s);
        // build approver
        AuthorityScopeApprover<S> sa = new AuthorityScopeApprover<>(s);

        if (asRealm) {
            // map all to realm role,
            // will work only for realm matching requests thanks to
            // user translation, ie a client can ask for a user to
            // consent scopes for managing the client's realm, if the user has those
            // authorities. We don't support a global client
            Set<? extends GrantedAuthority> authorities = s
                .getAuthorities()
                .stream()
                .map(a -> new SimpleGrantedAuthority(s.getRealm() + ":" + a))
                .collect(Collectors.toSet());
            sa.setGrantedAuthorities(authorities);
        } else {
            // ask exact match on global authority
            Set<? extends GrantedAuthority> authorities = s
                .getAuthorities()
                .stream()
                .map(a -> new SimpleGrantedAuthority(a))
                .collect(Collectors.toSet());
            sa.setGrantedAuthorities(authorities);
        }

        if (requireAll) {
            sa.setRequireAll(true);
        }

        setApprover(sa);
    }
}
