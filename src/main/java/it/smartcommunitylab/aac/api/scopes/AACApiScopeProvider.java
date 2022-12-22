package it.smartcommunitylab.aac.api.scopes;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.scope.approver.AuthorityScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractApiScopeProvider;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class AACApiScopeProvider extends AbstractApiScopeProvider<AbstractInternalApiScope> {

    public AACApiScopeProvider(AACApiResource resource) {
        super(SystemKeys.AUTHORITY_INTERNAL, resource.getProvider(), resource.getScopes());
        Assert.notNull(resource, "resource can not be null");
    }

    @Override
    protected AuthorityScopeApprover<AbstractInternalApiScope> buildScopeApprover(AbstractInternalApiScope s) {
        // build approver
        AuthorityScopeApprover<AbstractInternalApiScope> sa = new AuthorityScopeApprover<>(s);

        // map all to realm role,
        // will work only for realm matching requests thanks to
        // user translation, ie a client can ask for a user to
        // consent scopes for managing the client's realm, if the user has those
        // authorities. We don't support a global client
        Set<? extends GrantedAuthority> authorities = s.getAuthorities().stream()
                .map(a -> new RealmGrantedAuthority(s.getRealm(), a)).collect(Collectors.toSet());
        sa.setGrantedAuthorities(authorities);

        return sa;
    }

}
