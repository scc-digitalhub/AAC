package it.smartcommunitylab.aac.scope.approver;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApproval;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;
import it.smartcommunitylab.aac.scope.model.ApprovalStatus;
import it.smartcommunitylab.aac.scope.model.LimitedApiScopeApproval;

/*
 * A scope approver which requires consensus between all approvers.
 * 
 * Do note that a single MISS or DENY will suffice for negative responses.
 */

public class CombinedScopeApprover<S extends AbstractApiScope> extends AbstractScopeApprover<S, AbstractScopeApproval> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_DURATION_S = 3600; // 1h
    public static final int MIN_DURATION_S = 30; // 30s

    private List<AbstractScopeApprover<S, ? extends AbstractScopeApproval>> approvers;

    public CombinedScopeApprover(S scope, List<AbstractScopeApprover<S, ? extends AbstractScopeApproval>> approvers) {
        super(scope);

        setApprovers(approvers);
    }

    @SafeVarargs
    public CombinedScopeApprover(S scope, AbstractScopeApprover<S, ? extends AbstractScopeApproval>... approvers) {
        this(scope, Arrays.asList(approvers));
    }

    public void setApprovers(List<AbstractScopeApprover<S, ? extends AbstractScopeApproval>> approvers) {
        this.approvers = Collections.unmodifiableList(approvers);
    }

    @Override
    public AbstractScopeApproval approve(User user, ClientDetails client, Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        int duration = DEFAULT_DURATION_S;

        // get consensus for approve, or a single miss/deny
        for (ScopeApprover<? extends AbstractScopeApproval> approver : approvers) {
            AbstractScopeApproval appr = approver.approve(user, client, scopes);

            if (appr == null) {
                // lack of an approval is final
                return new LimitedApiScopeApproval(scope.getResourceId(), scope.getScope(),
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

        logger.debug("approve user {} for client {} with scopes {}: {}", user.getSubjectId(),
                client.getClientId(), String.valueOf(scopes), ApprovalStatus.APPROVED);

        // consensus from all approvers, build approval
        return new LimitedApiScopeApproval(scope.getResourceId(), scope.getScope(),
                user.getSubjectId(), client.getClientId(),
                duration, ApprovalStatus.APPROVED);
    }

    @Override
    public AbstractScopeApproval approve(ClientDetails client, Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        int duration = DEFAULT_DURATION_S;

        // get consensus for approve, or a single miss/deny
        for (ScopeApprover<? extends AbstractScopeApproval> approver : approvers) {
            AbstractScopeApproval appr = approver.approve(client, scopes);

            if (appr == null) {
                // lack of an approval is final
                return new LimitedApiScopeApproval(scope.getResourceId(), scope.getScope(),
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

        logger.debug("approve client {} with scopes {}: {}", client.getClientId(), String.valueOf(scopes),
                ApprovalStatus.APPROVED);

        // consensus from all approvers, build approval
        return new LimitedApiScopeApproval(scope.getResourceId(), scope.getScope(),
                client.getClientId(), client.getClientId(),
                duration, ApprovalStatus.APPROVED);
    }

}
