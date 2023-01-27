package it.smartcommunitylab.aac.scope.approver;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;
import it.smartcommunitylab.aac.scope.model.Scope;
import it.smartcommunitylab.aac.scope.model.ApprovalStatus;
import it.smartcommunitylab.aac.scope.model.LimitedApiScopeApproval;

public class AuthorityScopeApprover<S extends Scope> extends AbstractScopeApprover<S, LimitedApiScopeApproval> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_DURATION_S = 3600; // 1h

    private int duration;

    // authorities as string are interpreted as realm authorities
    private Set<String> authorities;

    // granted authorities are used as-is
    private Set<? extends GrantedAuthority> grantedAuthorities;

    private boolean requireAll = false;

    public AuthorityScopeApprover(S scope) {
        super(scope);
        Assert.notNull(scope, "scope can not be blank or null");

        this.duration = DEFAULT_DURATION_S;
        this.authorities = Collections.emptySet();
        this.grantedAuthorities = null;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setAuthorities(Set<String> authorities) {
        this.authorities = new HashSet<>(authorities);
    }

    public void setGrantedAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Assert.notNull(authorities, "authorities can not be null");
        this.grantedAuthorities = Collections.unmodifiableSet(new HashSet<>(authorities));
    }

    public void setRequireAll(boolean requireAll) {
        this.requireAll = requireAll;
    }

    @Override
    public LimitedApiScopeApproval approve(User user, ClientDetails client, Collection<String> scopes) {
        logger.debug("approve user {} for client {} with scopes {}", String.valueOf(user.getSubjectId()),
                String.valueOf(client.getClientId()), String.valueOf(scopes));

        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        boolean approved = false;
        Set<? extends GrantedAuthority> validAuthorities = grantedAuthorities;

        if (validAuthorities == null) {
            // build authorities from string definition for realm (or client realm)
            String r = realm != null ? realm : client.getRealm();
            validAuthorities = authorities.stream().map(a -> new RealmGrantedAuthority(r, a))
                    .collect(Collectors.toSet());
        }

        // evaluate authorities as-is
        Set<GrantedAuthority> userAuthorities = user.getAuthorities();
        if (requireAll) {
            // users need to possess all the defined authorities
            approved = validAuthorities.stream().allMatch(a -> userAuthorities.contains(a));
        } else {
            // we look for at least one
            approved = validAuthorities.stream().anyMatch(a -> userAuthorities.contains(a));
        }

        ApprovalStatus approvalStatus = approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;

        logger.debug("approve user {} for client {} with scopes {}: {}", user.getSubjectId(),
                client.getClientId(), String.valueOf(scopes), approvalStatus);

        return new LimitedApiScopeApproval(scope.getResourceId(), scope.getScope(),
                user.getSubjectId(), client.getClientId(),
                duration, approvalStatus);
    }

    @Override
    public LimitedApiScopeApproval approve(ClientDetails client, Collection<String> scopes) {
        logger.debug("approve client {} with scopes {}", String.valueOf(client.getClientId()), String.valueOf(scopes));

        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        boolean approved = false;
        Set<? extends GrantedAuthority> validAuthorities = grantedAuthorities;

        if (validAuthorities == null) {
            // build authorities from string definition for realm (or client realm)
            String r = realm != null ? realm : client.getRealm();
            validAuthorities = authorities.stream().map(a -> new RealmGrantedAuthority(r, a))
                    .collect(Collectors.toSet());
        }

        // evaluate authorities as-is
        Set<GrantedAuthority> clientAuthorities = client.getAuthorities();
        if (requireAll) {
            // clients need to possess all the defined authorities
            approved = validAuthorities.stream().allMatch(a -> clientAuthorities.contains(a));
        } else {
            // we look for at least one
            approved = validAuthorities.stream().anyMatch(a -> clientAuthorities.contains(a));
        }

        ApprovalStatus approvalStatus = approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;

        logger.debug("approve client {} with scopes {}: {}", client.getClientId(), String.valueOf(scopes),
                approvalStatus);

        return new LimitedApiScopeApproval(scope.getResourceId(), scope.getScope(),
                client.getClientId(), client.getClientId(),
                duration, approvalStatus);
    }

};