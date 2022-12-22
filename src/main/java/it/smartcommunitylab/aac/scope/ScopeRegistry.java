package it.smartcommunitylab.aac.scope;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.scope.model.ApiResource;
import it.smartcommunitylab.aac.scope.model.ApiResourceProvider;
import it.smartcommunitylab.aac.scope.model.ApiScope;
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
    private final ApiScopeProviderAuthorityService scopeAuthorityService;

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
    public ApiScope resolveScope(String realm, String scope) {
        // ask every provider
        // TODO improve
        return scopeAuthorityService.getAuthorities().stream()
                .flatMap(a -> a.getProvidersByRealm(realm).stream())
                .map(p -> p.findScopeByScope(scope)).findAny().orElse(null);
    }

    /*
     * Scopes as exposed by providers:
     * every provider can expose one or more resources
     * 
     */
    public ApiScope findScope(String realm, String scopeId) {
        ApiScopeProvider<?> sp = findScopeProvider(realm, scopeId);
        if (sp == null) {
            return null;
        }

        return sp.findScope(scopeId);
    }

    public ApiScope getScope(String realm, String scopeId) throws NoSuchScopeException {
        ApiScope s = findScope(realm, scopeId);
        if (s == null) {
            throw new NoSuchScopeException();
        }

        return s;
    }

    public ApiScopeProvider<? extends ApiScope> findScopeProvider(String realm, String scopeId) {
        // build provider id according to fixed schema
        String id = scopeId + "/" + realm;

        ApiScopeProvider<?> sp = scopeAuthorityService.getAuthorities().stream().map(a -> a.findProvider(id)).findAny()
                .orElse(null);
        if (sp == null) {
            return null;
        }

        return sp;
    }

    public ApiScopeProvider<? extends ApiScope> getScopeProvider(String realm, String scopeId)
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
    public ApiResource resolveResource(String realm, String resource) {
        // ask every provider
        // TODO improve
        return resourceAuthorityService.getAuthorities().stream()
                .flatMap(a -> a.getProvidersByRealm(realm).stream())
                .map(p -> p.findResourceByIdentifier(resource)).findAny().orElse(null);
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

    public ApiResourceProvider<? extends ApiResource> findResourceProvider(String realm, String resourceId) {
        // build provider id according to fixed schema
        String id = resourceId + "/" + realm;

        ApiResourceProvider<?> rp = resourceAuthorityService.getAuthorities().stream().map(a -> a.findProvider(id))
                .findAny()
                .orElse(null);
        if (rp == null) {
            return null;
        }

        return rp;
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
