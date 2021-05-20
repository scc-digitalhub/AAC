package it.smartcommunitylab.aac.scope;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;

/*
 * In-memory scope registry
 */

public class InMemoryScopeRegistry implements ScopeRegistry {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider registry is a map with keys matching resourceIds
    private final Map<String, ScopeProvider> providers = new HashMap<>();

    // create the register and populate will all providers
    public InMemoryScopeRegistry(Collection<ScopeProvider> scopeProviders) {

        // register internal scopeProviders to bootstrap
        // we don't care about resourceids at bootstrap
        for (ScopeProvider sp : scopeProviders) {
            _registerProvider(sp);
        }

    }

    @Override
    public void registerScopeProvider(ScopeProvider sp) {

        // check if aac scope, we don't want dynamic registration of core
        String resourceId = sp.getResourceId();
        if (resourceId != null && resourceId.startsWith("aac.")) {
            throw new IllegalArgumentException("can't register core scopes");
        }

        _registerProvider(sp);

    }

    @Override
    public void unregisterScopeProvider(ScopeProvider sp) {
        // check if aac scope, we don't want dynamic registration of core
        String resourceId = sp.getResourceId();
        if (resourceId != null && resourceId.startsWith("aac.")) {
            throw new IllegalArgumentException("can't register core scopes");
        }

        if (providers.containsKey(resourceId)) {
            // remove if matches registration
            providers.remove(resourceId, sp);
        }
    }

    @Override
    public ScopeProvider getScopeProviderFromScope(String scope) throws NoSuchScopeException {
        // get the first exporting the scope
        ScopeProvider provider = _getProvider(scope);
        if (provider == null) {
            throw new NoSuchScopeException();
        }

        return provider;
    }

    @Override
    public ScopeProvider findScopeProvider(String resourceId) {
        return providers.get(resourceId);
    }

    @Override
    public Collection<ScopeProvider> listScopeProviders() {
        return providers.values();
    }

    @Override
    public Scope findScope(String scope) {
        ScopeProvider sp = _getProvider(scope);
        if (sp == null) {
            return null;
        }

        return sp.getScopes().stream().filter(s -> s.getScope().equals(scope)).findFirst().orElse(null);

    }

    @Override
    public Scope getScope(String scope) throws NoSuchScopeException {
        Scope s = findScope(scope);
        if (s == null) {
            throw new NoSuchScopeException();
        }

        return s;
    }

    @Override
    public Collection<Scope> listScopes() {
        Set<Scope> result = new HashSet<>();
        providers.values().stream()
                .forEach(sp -> result.addAll(sp.getScopes()));

        return result;
    }

    @Override
    public Collection<Scope> listScopes(String resourceId) {
        if (providers.containsKey(resourceId)) {
            providers.get(resourceId).getScopes();
        }

        return Collections.emptyList();
    }

    /*
     * Internal
     */

    private ScopeProvider _getProvider(String scope) {
        Optional<ScopeProvider> provider = providers.values().stream()
                .filter(sp -> sp.getScopes()
                        .stream().anyMatch(s -> s.getScope().equals(scope)))
                .findFirst();

        if (provider.isPresent()) {
            return provider.get();
        }

        return null;
    }

    private void _registerProvider(ScopeProvider sp) {
        Collection<Scope> scopes = sp.getScopes();
        if (scopes == null) {
            throw new IllegalArgumentException("invalid scopes");
        }
        // validate all scopes
        if (!scopes.stream().allMatch(s -> validateScope(s))) {
            throw new IllegalArgumentException("invalid scopes");
        }

        if (!StringUtils.hasText(sp.getResourceId())) {
            throw new IllegalArgumentException("invalid resource id");
        }

        String resourceId = sp.getResourceId();
        logger.debug("register scope provider " + sp.toString() + " for resource " + resourceId);
        providers.put(resourceId, sp);

    }

    private boolean validateScope(Scope s) {
        if (s == null) {
            return false;
        }

        if (!StringUtils.hasText(s.getScope())) {
            return false;
        }

        if (s.getScope().length() < 3) {
            return false;
        }

        return true;

    }

    @Override
    public ScopeApprover getScopeApprover(String scope) throws NoSuchScopeException {
        ScopeProvider provider = _getProvider(scope);
        if (provider == null) {
            throw new NoSuchScopeException();
        }

        // approver may be null
        return provider.getApprover(scope);
    }

    @Override
    public Resource findResource(String resourceId) {
        if (providers.containsKey(resourceId)) {
            return providers.get(resourceId).getResource();
        }

        return null;
    }

    @Override
    public Resource getResource(String resourceId) throws NoSuchResourceException {
        Resource res = findResource(resourceId);
        if (res == null) {
            throw new NoSuchResourceException();
        }

        return res;
    }

    @Override
    public Collection<Resource> listResources() {
        return providers.values().stream()
                .map(p -> p.getResource())
                .collect(Collectors.toList());
    }

}
