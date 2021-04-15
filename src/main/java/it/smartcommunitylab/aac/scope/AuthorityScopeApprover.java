package it.smartcommunitylab.aac.scope;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.Approval.ApprovalStatus;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;

public class AuthorityScopeApprover implements ScopeApprover {

    public static final int DEFAULT_DURATION_MS = 3600000; // 1h

    private final String resourceId;
    private final String scope;
    private int duration;
    private Set<String> authorities;
    private boolean requireAll = false;

    public AuthorityScopeApprover(String resourceId, String scope) {
        Assert.hasText(resourceId, "resourceId can not be blank or null");
        Assert.hasText(scope, "scope can not be blank or null");
        this.resourceId = resourceId;
        this.scope = scope;
        this.authorities = Collections.emptySet();
        this.duration = DEFAULT_DURATION_MS;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setAuthorities(Set<String> authorities) {
        this.authorities = new HashSet<>(authorities);
    }

    public void setRequireAll(boolean requireAll) {
        this.requireAll = requireAll;
    }

    @Override
    public Approval approveUserScope(String scope, User user, ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException {
        if (!this.scope.equals(scope)) {
            return null;
        }

        Set<String> userAuthorities = user.getAuthorities().stream().map(a -> a.getAuthority())
                .collect(Collectors.toSet());

        boolean approved = false;
        if (requireAll) {
            // user needs to possess all the defined authorities
            approved = authorities.stream().allMatch(a -> userAuthorities.contains(a));
        } else {
            // we look for at least one
            approved = authorities.stream().anyMatch(a -> userAuthorities.contains(a));
        }

        ApprovalStatus approvalStatus = approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;
        return new Approval(resourceId, client.getClientId(), scope, duration, approvalStatus);
    }

    @Override
    public Approval approveClientScope(String scope, ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException {
        if (!this.scope.equals(scope)) {
            return null;
        }

        Set<String> clientAuthorities = client.getAuthorities().stream().map(a -> a.getAuthority())
                .collect(Collectors.toSet());

        boolean approved = false;
        if (requireAll) {
            // client needs to possess all the defined authorities
            approved = authorities.stream().allMatch(a -> clientAuthorities.contains(a));
        } else {
            // we look for at least one
            approved = authorities.stream().anyMatch(a -> clientAuthorities.contains(a));
        }

        ApprovalStatus approvalStatus = approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;
        return new Approval(resourceId, client.getClientId(), scope, duration, approvalStatus);
    }

};