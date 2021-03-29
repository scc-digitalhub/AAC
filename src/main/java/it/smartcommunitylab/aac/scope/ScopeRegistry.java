package it.smartcommunitylab.aac.scope;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/*
 * In-memory scope registry
 */

@Service
public class ScopeRegistry {

    // scope map
    private final Map<String, Scope> scopeRegistry = new HashMap<>();

    // TODO add provider map

    // create the register and populate will all providers
    public ScopeRegistry(List<ScopeProvider> scopeProviders) {

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

    public Scope findScope(String scope) {
        return scopeRegistry.get(scope);
    }

    public Collection<Scope> listScopes() {
        return scopeRegistry.values();
    }

    public Collection<Scope> listScopes(String resourceId) {
        return scopeRegistry.values().stream().filter(s -> resourceId.equals(s.getResourceId()))
                .collect(Collectors.toSet());
    }

}
