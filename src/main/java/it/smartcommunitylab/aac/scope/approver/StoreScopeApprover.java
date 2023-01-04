package it.smartcommunitylab.aac.scope.approver;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.springframework.security.oauth2.provider.approval.Approval;

import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;
import it.smartcommunitylab.aac.scope.model.ApiScope;
import it.smartcommunitylab.aac.scope.model.ApprovalStatus;
import it.smartcommunitylab.aac.scope.model.LimitedApiScopeApproval;

public class StoreScopeApprover<S extends ApiScope> extends AbstractScopeApprover<S, LimitedApiScopeApproval> {

    public static final int DEFAULT_DURATION_S = 3600; // 1h

    private SearchableApprovalStore approvalStore;

    private String userId;

    public StoreScopeApprover(S scope) {
        super(scope);

        // userId is used for store lookups
        this.userId = scope.getApiResourceId();
    }

    public void setApprovalStore(SearchableApprovalStore approvalStore) {
        this.approvalStore = approvalStore;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public LimitedApiScopeApproval approve(User user, ClientDetails client, Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        if (approvalStore == null) {
            return null;
        }

        Approval approval = approvalStore.findApproval(userId, user.getSubjectId(), scope.getScope());
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

        return new LimitedApiScopeApproval(scope.getApiResourceId(), scope.getScope(),
                user.getSubjectId(), client.getClientId(),
                expiresIn, approvalStatus);
    }

    @Override
    public LimitedApiScopeApproval approve(ClientDetails client, Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        if (approvalStore == null) {
            return null;
        }

        Approval approval = approvalStore.findApproval(userId, client.getClientId(), scope.getScope());
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

        return new LimitedApiScopeApproval(scope.getApiResourceId(), scope.getScope(),
                client.getClientId(), client.getClientId(),
                expiresIn, approvalStatus);
    }

}
