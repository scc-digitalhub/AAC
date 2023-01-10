package it.smartcommunitylab.aac.oauth.scope;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.scope.approver.AuthorityScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractScopeProvider;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class OAuth2DCRScopeProvider extends AbstractScopeProvider<AbstractInternalApiScope> {

    public OAuth2DCRScopeProvider(AbstractInternalApiScope s) {
        super(SystemKeys.AUTHORITY_OAUTH2, s.getProvider(), s.getRealm(), s);

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

        setApprover(sa);
    }

}
