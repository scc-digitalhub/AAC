package it.smartcommunitylab.aac.scope.approver;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.approval.Approval;

import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;
import it.smartcommunitylab.aac.scope.model.Scope;
import it.smartcommunitylab.aac.scope.model.ApprovalStatus;
import it.smartcommunitylab.aac.scope.model.LimitedApiScopeApproval;

public class StoreScopeApprover<S extends Scope> extends AbstractScopeApprover<S, LimitedApiScopeApproval> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_DURATION_S = 3600; // 1h

    private SearchableApprovalStore approvalStore;

    private String userId;

    public StoreScopeApprover(S scope) {
        super(scope);

        // userId is used for store lookups
        this.userId = scope.getResourceId();
    }

    public void setApprovalStore(SearchableApprovalStore approvalStore) {
        this.approvalStore = approvalStore;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public LimitedApiScopeApproval approve(User user, ClientDetails client, Collection<String> scopes) {
        logger.debug("approve user {} for client {} with scopes {}", String.valueOf(user.getSubjectId()),
                String.valueOf(client.getClientId()), String.valueOf(scopes));

        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        if (approvalStore == null) {
            logger.trace("approval store is null");
            return null;
        }

        Approval approval = approvalStore.findApproval(userId, user.getSubjectId(), scope.getScope());
        logger.trace("approval: {}", String.valueOf(approval));

        if (approval == null) {
            return null;
        }

        if (!approval.isCurrentlyActive()) {
            // cleanup expired
            approvalStore.revokeApprovals(Collections.singleton(approval));

            return null;
        }

        // build
        int expiresIn = DEFAULT_DURATION_S;
        if (approval.getExpiresAt() != null) {
            Date now = new Date();
            expiresIn = (int) ((approval.getExpiresAt().getTime() - now.getTime()) / 1000L);
        }

        ApprovalStatus approvalStatus = approval.isApproved() ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;

        logger.debug("approve user {} for client {} with scopes {}: {}", user.getSubjectId(),
                client.getClientId(), String.valueOf(scopes), approvalStatus);

        return new LimitedApiScopeApproval(scope.getResourceId(), scope.getScope(),
                user.getSubjectId(), client.getClientId(),
                expiresIn, approvalStatus);
    }

    @Override
    public LimitedApiScopeApproval approve(ClientDetails client, Collection<String> scopes) {
        logger.debug("approve client {} with scopes {}", String.valueOf(client.getClientId()), String.valueOf(scopes));

        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        if (approvalStore == null) {
            logger.trace("approval store is null");
            return null;
        }

        Approval approval = approvalStore.findApproval(userId, client.getClientId(), scope.getScope());
        logger.trace("approval: {}", String.valueOf(approval));

        if (approval == null) {
            return null;
        }

        if (!approval.isCurrentlyActive()) {
            // cleanup expired
            approvalStore.revokeApprovals(Collections.singleton(approval));

            return null;
        }

        // build
        int expiresIn = DEFAULT_DURATION_S;
        if (approval.getExpiresAt() != null) {
            Date now = new Date();
            expiresIn = (int) ((approval.getExpiresAt().getTime() - now.getTime()) / 1000L);
        }

        ApprovalStatus approvalStatus = approval.isApproved() ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;

        logger.debug("approve client {} with scopes {}: {}", client.getClientId(), String.valueOf(scopes),
                approvalStatus);

        return new LimitedApiScopeApproval(scope.getResourceId(), scope.getScope(),
                client.getClientId(), client.getClientId(),
                expiresIn, approvalStatus);
    }

}
