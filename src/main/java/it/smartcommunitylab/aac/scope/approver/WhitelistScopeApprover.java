package it.smartcommunitylab.aac.scope.approver;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;
import it.smartcommunitylab.aac.scope.model.Scope;
import it.smartcommunitylab.aac.scope.model.ApprovalStatus;
import it.smartcommunitylab.aac.scope.model.LimitedApiScopeApproval;

/*
 * An approver which will always return a positive result (no approval required)
 */
public class WhitelistScopeApprover<S extends Scope> extends AbstractScopeApprover<S, LimitedApiScopeApproval> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_DURATION_S = 21600; // 6h

    private int duration;

    public WhitelistScopeApprover(S scope) {
        super(scope);
        this.duration = DEFAULT_DURATION_S;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public LimitedApiScopeApproval approve(User user, ClientDetails client, Collection<String> scopes) {
        logger.debug("approve user {} for client {} with scopes {}", String.valueOf(user.getSubjectId()),
                String.valueOf(client.getClientId()), String.valueOf(scopes));

        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        logger.debug("approve user {} for client {} with scopes {}: {}", user.getSubjectId(),
                client.getClientId(), String.valueOf(scopes), ApprovalStatus.APPROVED);

        return new LimitedApiScopeApproval(scope.getResourceId(), scope.getScope(),
                user.getSubjectId(), client.getClientId(),
                duration, ApprovalStatus.APPROVED);
    }

    @Override
    public LimitedApiScopeApproval approve(ClientDetails client, Collection<String> scopes) {
        logger.debug("approve client {} with scopes {}", String.valueOf(client.getClientId()), String.valueOf(scopes));

        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        logger.debug("approve client {} with scopes {}: {}", client.getClientId(), String.valueOf(scopes),
                ApprovalStatus.APPROVED);

        return new LimitedApiScopeApproval(scope.getResourceId(), scope.getScope(),
                client.getClientId(), client.getClientId(),
                duration, ApprovalStatus.APPROVED);
    }

    public static class Builder<S extends Scope> {

        private S scope;
        private Integer duration;

        public Builder(S scope) {
            this.scope = scope;
        }

        public Builder<S> duration(int duration) {
            this.duration = duration;
            return this;
        }

        public WhitelistScopeApprover<S> build() {
            WhitelistScopeApprover<S> sa = new WhitelistScopeApprover<>(scope);
            if (duration != null) {
                sa.setDuration(this.duration);
            }

            return sa;
        }
    }

};