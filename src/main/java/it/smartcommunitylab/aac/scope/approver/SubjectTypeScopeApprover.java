package it.smartcommunitylab.aac.scope.approver;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.SubjectType;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;
import it.smartcommunitylab.aac.scope.model.Scope;
import it.smartcommunitylab.aac.scope.model.ApprovalStatus;
import it.smartcommunitylab.aac.scope.model.LimitedApiScopeApproval;

public class SubjectTypeScopeApprover<S extends Scope> extends AbstractScopeApprover<S, LimitedApiScopeApproval> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_DURATION_S = 3600; // 1h

    private int duration;
    private SubjectType subjectType;
    // strict mode when set blocks the request on denial,
    // otherwise we return undecided to let the processor skip the scope
    private boolean strictMode = false;

    public SubjectTypeScopeApprover(S scope) {
        super(scope);
        this.duration = DEFAULT_DURATION_S;
    }

    public void setSubjectType(SubjectType type) {
        Assert.notNull(type, "invalid subject type");
        if (SubjectType.USER != type && SubjectType.CLIENT != type) {
            throw new IllegalArgumentException("invalid subject type: set either user or client");
        }

        this.subjectType = type;
    }

    public void setSubjectType(String subjectType) {
        Assert.hasText(subjectType, "subject type can not be null or empty");
        setSubjectType(SubjectType.parse(subjectType));
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
        logger.debug("approve user {} for client {} with scopes {}", String.valueOf(user.getSubjectId()),
                String.valueOf(client.getClientId()), String.valueOf(scopes));

        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        ApprovalStatus approvalStatus = SubjectType.USER == subjectType ? ApprovalStatus.APPROVED
                : ApprovalStatus.DENIED;

        if (!strictMode && approvalStatus == ApprovalStatus.DENIED) {
            // return undecided
            return null;
        }

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

        ApprovalStatus approvalStatus = SubjectType.CLIENT == subjectType ? ApprovalStatus.APPROVED
                : ApprovalStatus.DENIED;

        if (!strictMode && approvalStatus == ApprovalStatus.DENIED) {
            // return undecided
            return null;
        }

        logger.debug("approve client {} with scopes {}: {}", client.getClientId(), String.valueOf(scopes),
                approvalStatus);

        return new LimitedApiScopeApproval(scope.getResourceId(), scope.getScope(),
                client.getClientId(), client.getClientId(),
                duration, approvalStatus);
    }

}
