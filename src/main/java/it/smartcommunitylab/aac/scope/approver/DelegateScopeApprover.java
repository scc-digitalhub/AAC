package it.smartcommunitylab.aac.scope.approver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;
import it.smartcommunitylab.aac.scope.model.ApiScope;
import it.smartcommunitylab.aac.scope.model.ScopeApproval;

/*
 * A scope approver which requires at least one of the provided approvers to respond with a positive authorization result.
 * 
 * Do note that this approver will never return a DENY
 */

public class DelegateScopeApprover<S extends ApiScope> extends AbstractScopeApprover<S, ScopeApproval> {

    private List<ScopeApprover<? extends ScopeApproval>> approvers;

    public DelegateScopeApprover(S scope, List<ScopeApprover<? extends ScopeApproval>> approvers) {
        super(scope);
        setApprovers(approvers);
    }

    @SafeVarargs
    public DelegateScopeApprover(S scope, ScopeApprover<? extends ScopeApproval>... approvers) {
        this(scope, Arrays.asList(approvers));
    }

    public void setApprovers(List<ScopeApprover<? extends ScopeApproval>> approvers) {
        this.approvers = new ArrayList<>(approvers);
    }

    @Override
    public ScopeApproval approve(User user, ClientDetails client, Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        // get first positive approval, we discard null
        // we ignore duration because for caching purposes any valid result will
        // still be present before expiration date
        Optional<? extends ScopeApproval> appr = approvers.stream()
                .map(ap -> ap.approve(client, scopes))
                .filter(a -> a.isApproved())
                .findAny();

        if (appr.isPresent()) {
            return (ScopeApproval) appr.get();
        }

        // undecided
        return null;
    }

    @Override
    public ScopeApproval approve(ClientDetails client, Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        // get first positive approval, we discard null
        // we ignore duration because for caching purposes any valid result will
        // still be present before expiration date
        Optional<? extends ScopeApproval> appr = approvers.stream()
                .map(ap -> ap.approve(client, scopes))
                .filter(a -> a.isApproved())
                .findAny();

        if (appr.isPresent()) {
            return (ScopeApproval) appr.get();
        }

        // undecided
        return null;
    }

}
