package it.smartcommunitylab.aac.scope.approver;

import java.util.Collection;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;
import it.smartcommunitylab.aac.scope.model.ApiScope;
import it.smartcommunitylab.aac.scope.model.ApprovalStatus;
import it.smartcommunitylab.aac.scope.model.LimitedApiScopeApproval;

public class SubjectTypeScopeApprover<S extends ApiScope> extends AbstractScopeApprover<S, LimitedApiScopeApproval> {

    public static final int DEFAULT_DURATION_S = 3600; // 1h

    private int duration;

    private String subjectType;

    public SubjectTypeScopeApprover(S scope) {
        super(scope);
        this.duration = DEFAULT_DURATION_S;
    }

    public void setSubjectType(String subjectType) {
        Assert.hasText(subjectType, "subject type can not be null or empty");
        if (!SystemKeys.RESOURCE_USER.equals(subjectType) && !SystemKeys.RESOURCE_CLIENT.equals(subjectType)) {
            throw new IllegalArgumentException("invalid subject type: set either user or client");
        }

        this.subjectType = subjectType;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public LimitedApiScopeApproval approve(User user, ClientDetails client, Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        ApprovalStatus approvalStatus = SystemKeys.RESOURCE_USER.equals(subjectType) ? ApprovalStatus.APPROVED
                : ApprovalStatus.DENIED;

        return new LimitedApiScopeApproval(scope.getApiResourceId(), scope.getScope(),
                user.getSubjectId(), client.getClientId(),
                duration, approvalStatus);
    }

    @Override
    public LimitedApiScopeApproval approve(ClientDetails client, Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        ApprovalStatus approvalStatus = SystemKeys.RESOURCE_CLIENT.equals(subjectType) ? ApprovalStatus.APPROVED
                : ApprovalStatus.DENIED;

        return new LimitedApiScopeApproval(scope.getApiResourceId(), scope.getScope(),
                client.getClientId(), client.getClientId(),
                duration, approvalStatus);
    }

}
