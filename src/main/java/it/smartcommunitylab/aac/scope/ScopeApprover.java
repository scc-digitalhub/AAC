package it.smartcommunitylab.aac.scope;

import java.util.Collection;

import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.model.ApiScopeApproval;

/*
 * A scope approver can decide if the requested scope can be obtained by applying any reasoning.
 * 
 * When the approver can make a decision, the result will contain the response (approve/deny).
 * An approve will authorize the release of the scope, while a deny will make the request fail.
 * 
 * Returning <null> will indicate that the approver can't decide, or is willing to let the request continue without this scope.
 * This happens because scopes without approval will be dropped from the request.
 * 
 * Do note that we don't make assumptions on the actual implementation, the registry won't keep a copy of the approver 
 * but ask the providers for every request.
 */

public interface ScopeApprover<A extends ApiScopeApproval> extends ResourceProvider<A> {

    public String getScope();

    // TODO replace user and client with UserContext, ClientContext
    public A approve(User user, ClientDetails client, Collection<String> scopes);

    public A approve(ClientDetails client, Collection<String> scopes);
}
