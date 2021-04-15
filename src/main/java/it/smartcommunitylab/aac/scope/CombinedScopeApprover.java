package it.smartcommunitylab.aac.scope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.Approval.ApprovalStatus;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;

/*
 * A scope approver which requires consensus between all approvers.
 * 
 * Do note that a single DENY will suffice for negative responses.
 */

public class CombinedScopeApprover implements ScopeApprover {
    public static final int DEFAULT_DURATION_MS = 3600000; // 1h

    private final String resourceId;
    private final String scope;

    private List<ScopeApprover> approvers;

    public CombinedScopeApprover(String resourceId, String scope, ScopeApprover... approvers) {
        this(resourceId, scope, Arrays.asList(approvers));
    }

    public CombinedScopeApprover(String resourceId, String scope, List<ScopeApprover> approvers) {
        Assert.notNull(approvers, "approvers can not be null");
        Assert.hasText(resourceId, "resourceId can not be blank or null");
        Assert.hasText(scope, "scope can not be blank or null");
        this.resourceId = resourceId;
        this.scope = scope;
        setApprovers(approvers);
    }

    public void setApprovers(List<ScopeApprover> approvers) {
        this.approvers = new ArrayList<>(approvers);
    }

    @Override
    public Approval approveUserScope(String scope, User user, ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException {
        if (!this.scope.equals(scope)) {
            return null;
        }

        // get consensus for approve, or a single deny
        List<Approval> approvals = new ArrayList<>();
        for (ScopeApprover approver : approvers) {
            Approval appr = approver.approveUserScope(scope, user, client, scopes);
            if (appr != null) {
                if (!appr.isApproved()) {
                    // deny is final
                    return appr;
                } else {
                    // save positive approvals
                    approvals.add(appr);
                }
            }
        }

        Approval appr = null;
        if (!approvals.isEmpty()) {
            // get min duration
            int duration = DEFAULT_DURATION_MS;
            Optional<Date> expires = approvals.stream().map(a -> a.getExpiresAt()).sorted().findFirst();
            Date now = new Date();
            if (expires.isPresent()) {
                duration = (int) Math.abs(expires.get().getTime() - now.getTime());
            }

            appr = new Approval(resourceId, client.getClientId(), scope, duration, ApprovalStatus.APPROVED);
        }

        return appr;
    }

    @Override
    public Approval approveClientScope(String scope, ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException {
        if (!this.scope.equals(scope)) {
            return null;
        }

        // get consensus for approve, or a single deny
        List<Approval> approvals = new ArrayList<>();
        for (ScopeApprover approver : approvers) {
            Approval appr = approver.approveClientScope(scope, client, scopes);
            if (appr != null) {
                if (!appr.isApproved()) {
                    // deny is final
                    return appr;
                } else {
                    // save positive approvals
                    approvals.add(appr);
                }
            }
        }

        Approval appr = null;
        if (!approvals.isEmpty()) {
            // get min duration
            int duration = DEFAULT_DURATION_MS;
            Optional<Date> expires = approvals.stream().map(a -> a.getExpiresAt()).sorted().findFirst();
            Date now = new Date();
            if (expires.isPresent()) {
                duration = (int) Math.abs(expires.get().getTime() - now.getTime());
            }

            appr = new Approval(resourceId, client.getClientId(), scope, duration, ApprovalStatus.APPROVED);
        }

        return appr;
    }
}
