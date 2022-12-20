package it.smartcommunitylab.aac.scope.approver;

import java.util.Collection;

import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;
import it.smartcommunitylab.aac.scope.model.ApiScope;
import it.smartcommunitylab.aac.scope.model.ApprovalStatus;
import it.smartcommunitylab.aac.scope.model.LimitedScopeApproval;

/*
 * An approver which will always return a positive result (no approval required)
 */
public class WhitelistScopeApprover<S extends ApiScope> extends AbstractScopeApprover<S, LimitedScopeApproval> {

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
    public LimitedScopeApproval approve(User user, ClientDetails client, Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        return new LimitedScopeApproval(scope.getApiResourceId(), scope.getScope(),
                user.getSubjectId(), client.getClientId(),
                duration, ApprovalStatus.APPROVED);
    }

    @Override
    public LimitedScopeApproval approve(ClientDetails client, Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        return new LimitedScopeApproval(scope.getApiResourceId(), scope.getScope(),
                client.getClientId(), client.getClientId(),
                duration, ApprovalStatus.APPROVED);
    }

    public static class Builder<S extends ApiScope> {

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