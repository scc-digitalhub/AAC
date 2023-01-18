package it.smartcommunitylab.aac.scope;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.scope.model.ApiResource;
import it.smartcommunitylab.aac.scope.model.ApiResourceProvider;
import it.smartcommunitylab.aac.scope.model.Scope;
import it.smartcommunitylab.aac.scope.model.ApiScopeProvider;

/*
 * A registry for scopes
 * 
 * We don't expect providers to be immutable:
 * the registry implementations are supposed to retrieve scopes from providers at each request.
 * 
 */

@Service
public class ScopeRegistry {

    private final ApiResourceProviderAuthorityService resourceAuthorityService;

    public ScopeRegistry(
            ApiResourceProviderAuthorityService resourceAuthorityService,
            ApiScopeProviderAuthorityService scopeAuthorityService) {
        Assert.notNull(resourceAuthorityService, "api resource authority service is required");
        Assert.notNull(scopeAuthorityService, "api scope authority service is required");

        this.resourceAuthorityService = resourceAuthorityService;
        this.scopeAuthorityService = scopeAuthorityService;
    }

    /*
     * Scopes according to OAuth2
     */
    public Scope tryResolveScope(String realm, String scope) {
        // ask every provider
        // TODO improve
        return scopeAuthorityService.getAuthorities().stream()
                .flatMap(a -> a.getProvidersByRealm(realm).stream())
                .map(p -> p.findScopeByScope(scope))
                .filter(p -> p != null)
                .findAny().orElse(null);
    }

    public Scope resolveScope(String realm, String scope) throws NoSuchScopeException {
        Scope s = tryResolveScope(realm, scope);
        if (s == null) {
            throw new NoSuchScopeException();
        }

        return s;
    }

    public Scope resolveScopeById(String realm, String scopeId) {
        // ask every provider
        // TODO improve
        return scopeAuthorityService.getAuthorities().stream()
                .flatMap(a -> a.getProvidersByRealm(realm).stream())
                .map(p -> p.findScope(scopeId))
                .filter(p -> p != null)
                .findAny().orElse(null);
    }

    /*
     * Scopes as exposed by providers:
     * every provider can expose one or more resources
     * 
     */
    public Scope findScope(String realm, String scopeId) {
        ApiScopeProvider<?> sp = findScopeProvider(realm, scopeId);
        if (sp == null) {
            return null;
        }

        return sp.findScope(scopeId);
    }

    public Scope getScope(String realm, String scopeId) throws NoSuchScopeException {
        Scope s = findScope(realm, scopeId);
        if (s == null) {
            throw new NoSuchScopeException();
        }

        return s;
    }

    public Collection<Scope> listScopes(String realm) {
        // ask every provider
        // TODO improve
        return scopeAuthorityService.getAuthorities().stream()
                .flatMap(a -> a.getProvidersByRealm(realm).stream())
                .flatMap(p -> p.listScopes().stream())
                .collect(Collectors.toList());
    }

    public ApiScopeProvider<? extends Scope> findScopeProvider(String realm, String scopeId) {
        // resolve scope to fetch provider
        Scope s = resolveScopeById(realm, scopeId);
        if (s == null) {
            return null;
        }

        String provider = s.getProvider();

        // pick first provider
        // TODO improve logic
        return scopeAuthorityService.getAuthorities().stream()
                .map(a -> a.findProvider(provider)).filter(p -> p != null).findAny().orElse(null);
    }

    public ApiScopeProvider<? extends Scope> getScopeProvider(String realm, String scopeId)
            throws NoSuchScopeException {
        ApiScopeProvider<?> sp = findScopeProvider(realm, scopeId);
        if (sp == null) {
            throw new NoSuchScopeException();
        }

        return sp;
    }

    /*
     * Resources according to OAuth2
     */
    public ApiResource tryResolveResource(String realm, String resource) {
        // ask every provider
        // TODO improve
        return resourceAuthorityService.getAuthorities().stream()
                .flatMap(a -> a.getProvidersByRealm(realm).stream())
                .map(p -> p.findResourceByIdentifier(resource))
                .filter(p -> p != null)
                .findAny().orElse(null);
    }

    public ApiResource resolveResource(String realm, String resource) throws NoSuchResourceException {
        ApiResource r = tryResolveResource(realm, resource);
        if (r == null) {
            throw new NoSuchResourceException();
        }

        return r;
    }

    public ApiResource resolveResourceById(String realm, String resourceId) {
        // ask every provider
        // TODO improve
        return resourceAuthorityService.getAuthorities().stream()
                .flatMap(a -> a.getProvidersByRealm(realm).stream())
                .map(p -> p.findResource(resourceId))
                .filter(p -> p != null)
                .findAny().orElse(null);
    }

    /*
     * Resources as exposed by providers:
     * every provider can expose one or more resources
     */
    public ApiResource findResource(String realm, String resourceId) {
        ApiResourceProvider<?> sp = findResourceProvider(realm, resourceId);
        if (sp == null) {
            return null;
        }

        return sp.findResource(resourceId);
    }

    public ApiResource getResource(String realm, String resourceId) throws NoSuchResourceException {
        ApiResource s = findResource(realm, resourceId);
        if (s == null) {
            throw new NoSuchResourceException();
        }

        return s;
    }

    public Collection<ApiResource> listResources(String realm) {
        // ask every provider
        // TODO improve
        return resourceAuthorityService.getAuthorities().stream()
                .flatMap(a -> a.getProvidersByRealm(realm).stream())
                .flatMap(p -> p.listResources().stream())
                .collect(Collectors.toList());
    }

    public ApiResourceProvider<? extends ApiResource> findResourceProvider(String realm, String resourceId) {
        // resolve resource to fetch provider
        ApiResource r = resolveResourceById(realm, resourceId);
        if (r == null) {
            return null;
        }

        String provider = r.getProvider();

        // pick first provider
        // TODO improve logic
        return resourceAuthorityService.getAuthorities().stream()
                .map(a -> a.findProvider(provider)).filter(p -> p != null).findAny().orElse(null);
    }

    public ApiResourceProvider<? extends ApiResource> getResourceProvider(String realm, String resourceId)
            throws NoSuchResourceException {
        ApiResourceProvider<?> rp = findResourceProvider(realm, resourceId);
        if (rp == null) {
            throw new NoSuchResourceException();
        }

        return rp;
    }
//    /*
//     * Scope providers
//     */
//    public void registerScopeProvider(ScopeProvider sp);
//
//    public void unregisterScopeProvider(ScopeProvider sp);
//
//    public ScopeProvider findScopeProvider(String resourceId);
//
//    public ScopeProvider getScopeProviderFromScope(String scope) throws NoSuchScopeException;
//
//    public Collection<ScopeProvider> listScopeProviders();
//
//    /*
//     * Scopes as exposed by providers
//     */
//
//    public Scope findScope(String scope);
//
//    public Scope getScope(String scope) throws NoSuchScopeException;
//
//    public Collection<Scope> listScopes();
//
//    public Collection<Scope> listScopes(String resourceId);
//
//    /*
//     * Approvers are exposed by providers
//     */
//    public ScopeApprover getScopeApprover(String scope) throws NoSuchScopeException;
//
//    /*
//     * Resources as exposed by providers
//     */
//    public ApiResource findResource(String resourceId);
//
//    public ApiResource getResource(String resourceId) throws NoSuchResourceException;
//
//    public Collection<ApiResource> listResources();

}
