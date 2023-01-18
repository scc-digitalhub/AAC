package it.smartcommunitylab.aac.scope.approver;

import java.util.Collection;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;
import it.smartcommunitylab.aac.scope.model.Scope;
import it.smartcommunitylab.aac.scope.model.ApprovalStatus;
import it.smartcommunitylab.aac.scope.model.LimitedApiScopeApproval;

public class SubjectTypeScopeApprover<S extends Scope> extends AbstractScopeApprover<S, LimitedApiScopeApproval> {

    public static final int DEFAULT_DURATION_S = 3600; // 1h

    private int duration;
    private String subjectType;
    // strict mode when set blocks the request on denial,
    // otherwise we return undecided to let the processor skip the scope
    private boolean strictMode = false;

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

    public boolean isStrictMode() {
        return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    @Override
    public LimitedApiScopeApproval approve(User user, ClientDetails client, Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        ApprovalStatus approvalStatus = SystemKeys.RESOURCE_USER.equals(subjectType) ? ApprovalStatus.APPROVED
                : ApprovalStatus.DENIED;

        if (!strictMode && approvalStatus == ApprovalStatus.DENIED) {
            // return undecided
            return null;
        }

        return new LimitedApiScopeApproval(scope.getResourceId(), scope.getScope(),
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

        if (!strictMode && approvalStatus == ApprovalStatus.DENIED) {
            // return undecided
            return null;
        }

        return new LimitedApiScopeApproval(scope.getResourceId(), scope.getScope(),
                client.getClientId(), client.getClientId(),
                duration, approvalStatus);
    }

}
