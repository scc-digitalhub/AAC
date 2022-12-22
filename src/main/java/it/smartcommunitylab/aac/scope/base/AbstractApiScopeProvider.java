package it.smartcommunitylab.aac.scope.base;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.common.NoSuchScopeException;

public abstract class AbstractApiScopeProvider<S extends AbstractApiScope> extends AbstractScopeProvider<S> {

    private final Map<String, S> scopes;
    private final Map<String, AbstractScopeApprover<S, ?>> approvers;

    @SafeVarargs
    public AbstractApiScopeProvider(String authority, String provider, S... scopes) {
        this(authority, provider, Arrays.asList(scopes));
    }

    public AbstractApiScopeProvider(String authority, String provider, Collection<S> scopes) {
        super(authority, provider);

        // map scopes by id
        this.scopes = scopes.stream()
                .collect(Collectors.toMap(s -> s.getScopeId(), s -> s));

        // init approvers map
        approvers = new HashMap<>();
    }

    @Override
    public S findScopeByScope(String scope) {
        return scopes.values().stream().filter(s -> s.getScope().equals(scope)).findFirst().orElse(null);
    }

    @Override
    public S findScope(String scopeId) {
        return scopes.get(scopeId);
    }

    @Override
    public S getScope(String scopeId) throws NoSuchScopeException {
        S s = findScope(scopeId);
        if (s == null) {
            throw new NoSuchScopeException();
        }

        return s;
    }

    @Override
    public Collection<S> listScopes() {
        return Collections.unmodifiableCollection(scopes.values());
    }

    @Override
    public AbstractScopeApprover<S, ?> getScopeApprover(String scopeId) throws NoSuchScopeException {
        S s = getScope(scopeId);
        if (!approvers.containsKey(scopeId)) {
            AbstractScopeApprover<S, ?> sa = buildScopeApprover(s);
            approvers.put(scopeId, sa);
        }

        return approvers.get(scopeId);
    }

    protected abstract AbstractScopeApprover<S, ?> buildScopeApprover(S scope);

}