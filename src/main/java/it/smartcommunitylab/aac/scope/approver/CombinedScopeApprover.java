package it.smartcommunitylab.aac.scope.approver;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;
import it.smartcommunitylab.aac.scope.model.ApiScope;
import it.smartcommunitylab.aac.scope.model.ApprovalStatus;
import it.smartcommunitylab.aac.scope.model.LimitedScopeApproval;
import it.smartcommunitylab.aac.scope.model.ScopeApproval;

/*
 * A scope approver which requires consensus between all approvers.
 * 
 * Do note that a single MISS or DENY will suffice for negative responses.
 */

public class CombinedScopeApprover<S extends ApiScope> extends AbstractScopeApprover<S, ScopeApproval> {
    public static final int DEFAULT_DURATION_S = 3600; // 1h
    public static final int MIN_DURATION_S = 30; // 30s

    private List<ScopeApprover<? extends ScopeApproval>> approvers;

    public CombinedScopeApprover(S scope, List<ScopeApprover<? extends ScopeApproval>> approvers) {
        super(scope);

        setApprovers(approvers);
    }

    @SafeVarargs
    public CombinedScopeApprover(S scope, ScopeApprover<? extends ScopeApproval>... approvers) {
        this(scope, Arrays.asList(approvers));
    }

    public void setApprovers(List<ScopeApprover<? extends ScopeApproval>> approvers) {
        this.approvers = Collections.unmodifiableList(approvers);
    }

    @Override
    public ScopeApproval approve(User user, ClientDetails client, Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        int duration = DEFAULT_DURATION_S;

        // get consensus for approve, or a single miss/deny
        for (ScopeApprover<? extends ScopeApproval> approver : approvers) {
            ScopeApproval appr = approver.approve(user, client, scopes);

            if (appr == null) {
                // lack of an approval is final
                return new LimitedScopeApproval(scope.getApiResourceId(), scope.getScope(),
                        user.getSubjectId(), client.getClientId(),
                        MIN_DURATION_S, ApprovalStatus.DENIED);
            }

            if (!appr.isApproved()) {
                // deny is final
                return appr;
            } else {
                // keep min duration
                long expiresIn = appr.expiresIn();
                if (expiresIn > 0 && expiresIn < duration) {
                    duration = (int) expiresIn;
                }
            }
        }

        // consensus from all approvers, build approval
        return new LimitedScopeApproval(scope.getApiResourceId(), scope.getScope(),
                user.getSubjectId(), client.getClientId(),
                duration, ApprovalStatus.APPROVED);
    }

    @Override
    public ScopeApproval approve(ClientDetails client, Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        int duration = DEFAULT_DURATION_S;

        // get consensus for approve, or a single miss/deny
        for (ScopeApprover<? extends ScopeApproval> approver : approvers) {
            ScopeApproval appr = approver.approve(client, scopes);

            if (appr == null) {
                // lack of an approval is final
                return new LimitedScopeApproval(scope.getApiResourceId(), scope.getScope(),
                        client.getClientId(), client.getClientId(),
                        MIN_DURATION_S, ApprovalStatus.DENIED);
            }

            if (!appr.isApproved()) {
                // deny is final
                return appr;
            } else {
                // keep min duration
                long expiresIn = appr.expiresIn();
                if (expiresIn > 0 && expiresIn < duration) {
                    duration = (int) expiresIn;
                }
            }
        }

        // consensus from all approvers, build approval
        return new LimitedScopeApproval(scope.getApiResourceId(), scope.getScope(),
                client.getClientId(), client.getClientId(),
                duration, ApprovalStatus.APPROVED);
    }

}
