package it.smartcommunitylab.aac.scope.approver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApproval;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;
import it.smartcommunitylab.aac.scope.model.Scope;

/*
 * A scope approver which requires at least one of the provided approvers to respond with a positive authorization result.
 * 
 * Do note that this approver will never return a DENY
 */

public class DelegateScopeApprover<S extends Scope> extends AbstractScopeApprover<S, AbstractScopeApproval> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private List<ScopeApprover<? extends AbstractScopeApproval>> approvers;

    public DelegateScopeApprover(S scope, List<AbstractScopeApprover<S, ? extends AbstractScopeApproval>> approvers) {
        super(scope);
        setApprovers(approvers);
    }

    @SafeVarargs
    public DelegateScopeApprover(S scope, AbstractScopeApprover<S, ? extends AbstractScopeApproval>... approvers) {
        this(scope, Arrays.asList(approvers));
    }

    public void setApprovers(List<AbstractScopeApprover<S, ? extends AbstractScopeApproval>> approvers) {
        this.approvers = new ArrayList<>(approvers);
    }

    @Override
    public AbstractScopeApproval approve(User user, ClientDetails client, Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        // get first positive approval, we discard null
        // we ignore duration because for caching purposes any valid result will
        // still be present before expiration date
        Optional<? extends AbstractScopeApproval> appr = approvers.stream()
                .map(ap -> ap.approve(client, scopes))
                .filter(a -> a.isApproved())
                .findAny();

        if (appr.isPresent()) {
            logger.debug("approve user {} for client {} with scopes {}: {}", user.getSubjectId(),
                    client.getClientId(), String.valueOf(scopes), appr.get().getApproval().getStatus());

            return appr.get();
        }

        // undecided
        return null;
    }

    @Override
    public AbstractScopeApproval approve(ClientDetails client, Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        // get first positive approval, we discard null
        // we ignore duration because for caching purposes any valid result will
        // still be present before expiration date
        Optional<? extends AbstractScopeApproval> appr = approvers.stream()
                .map(ap -> ap.approve(client, scopes))
                .filter(a -> a.isApproved())
                .findAny();

        if (appr.isPresent()) {
            logger.debug("approve client {} with scopes {}: {}", client.getClientId(), String.valueOf(scopes),
                    appr.get().getApproval().getStatus());

            return appr.get();
        }

        // undecided
        return null;
    }

}
