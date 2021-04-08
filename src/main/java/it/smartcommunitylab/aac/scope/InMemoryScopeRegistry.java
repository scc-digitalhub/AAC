package it.smartcommunitylab.aac.scope;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchScopeException;

/*
 * In-memory scope registry
 */

public class InMemoryScopeRegistry implements ScopeRegistry {

    // scope map
    private final Map<String, Scope> scopeRegistry = new HashMap<>();

    // TODO add provider map

    // create the register and populate will all providers
    public InMemoryScopeRegistry(Collection<ScopeProvider> scopeProviders) {

        // register internal scopeProviders to bootstrap
        // we don't care about resourceids at bootstrap
        for (ScopeProvider sp : scopeProviders) {
            _registerScopes(sp.getScopes());
        }

    }

    private void _registerScope(Scope s) {
        String scope = s.getScope();
        if (scopeRegistry.containsKey(scope)) {
            throw new IllegalArgumentException("scope is already registered");
        }

        scopeRegistry.put(scope, s);
    }

    private void _registerScopes(Collection<Scope> scopes) {
        for (Scope s : scopes) {
            _registerScope(s);
        }
    }

    @Override
    public void registerScope(Scope s) {
        String scope = s.getScope();
        if (!StringUtils.hasText(scope)) {
            throw new IllegalArgumentException("invalid scope");
        }

        // check if aac scope, we don't want dynamic registration of core
        String resourceId = s.getResourceId();
        if (resourceId != null && resourceId.startsWith("aac.")) {
            throw new IllegalArgumentException("can't register core scopes");
        }

        _registerScope(s);

    }

    @Override
    public void unregisterScope(Scope s) {
        if (s != null) {
            unregisterScope(s.getScope());
        }
    }

    public void unregisterScope(String scope) {
        if (scopeRegistry.containsKey(scope)) {

            // check if aac scope, we don't want dynamic registration of core
            Scope s = scopeRegistry.get(scope);
            String resourceId = s.getResourceId();
            if (resourceId != null && resourceId.startsWith("aac.")) {
                throw new IllegalArgumentException("can't register core scopes");
            }

            scopeRegistry.remove(scope);
        }
    }

    @Override
    public Scope findScope(String scope) {
        return scopeRegistry.get(scope);
    }

    @Override
    public Scope getScope(String scope) throws NoSuchScopeException {
        Scope s = scopeRegistry.get(scope);
        if (s == null) {
            throw new NoSuchScopeException();
        }

        return s;
    }

    @Override
    public Collection<Scope> listScopes() {
        return scopeRegistry.values();
    }

    @Override
    public Collection<Scope> listScopes(String resourceId) {
        return scopeRegistry.values().stream().filter(s -> resourceId.equals(s.getResourceId()))
                .collect(Collectors.toSet());
    }

}
