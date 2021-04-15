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

public class RoleScopeApprover implements ScopeApprover {

    public static final int DEFAULT_DURATION_MS = 3600000; // 1h

    private final String realm;
    private final String resourceId;
    private final String scope;
    private int duration;
    private Set<String> roles;
    private boolean requireAll = false;

    public RoleScopeApprover(String realm, String resourceId, String scope) {
        Assert.notNull(realm, "realm can not be null");
        Assert.hasText(resourceId, "resourceId can not be blank or null");
        Assert.hasText(scope, "scope can not be blank or null");
        this.realm = realm;
        this.resourceId = resourceId;
        this.scope = scope;
        this.roles = Collections.emptySet();
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setRoles(Set<String> roles) {
        this.roles = new HashSet<>(roles);
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

        Set<String> userRoles = user.getRoles().stream().map(r -> r.getAuthority())
                .collect(Collectors.toSet());

        boolean approved = false;
        if (requireAll) {
            // user needs to possess all the defined roles
            approved = roles.stream().allMatch(a -> userRoles.contains(a));
        } else {
            // we look for at least one
            approved = roles.stream().anyMatch(a -> userRoles.contains(a));
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

        // TODO update after replacing ClientDetails with Client model including roles
//        Set<String> clientRoles = client.getRoles().stream().map(r -> r.getAuthority())
//                .collect(Collectors.toSet());

        boolean approved = false;
//        if (requireAll) {
//            // client needs to possess all the defined roles
//            approved = roles.stream().allMatch(a -> clientRoles.contains(a));
//        } else {
//            // we look for at least one
//            approved = roles.stream().anyMatch(a -> clientRoles.contains(a));
//        }

        ApprovalStatus approvalStatus = approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;
        return new Approval(resourceId, client.getClientId(), scope, duration, approvalStatus);
    }

    @Override
    public String getRealm() {
        return realm;
    }
};