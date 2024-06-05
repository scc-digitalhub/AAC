package it.smartcommunitylab.aac.scope;

import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.scope.model.ApiResource;
import it.smartcommunitylab.aac.scope.model.ApiResourceProvider;
import it.smartcommunitylab.aac.scope.model.ApiScopeProvider;
import it.smartcommunitylab.aac.scope.model.Scope;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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

    public ScopeRegistry(ApiResourceProviderAuthorityService resourceAuthorityService) {
        Assert.notNull(resourceAuthorityService, "api resource authority service is required");

        this.resourceAuthorityService = resourceAuthorityService;
    }

    /*
     * Scopes according to OAuth2
     */
    public Scope tryResolveScope(String realm, String scope) {
        // ask every provider
        // TODO improve
        return resourceAuthorityService
            .getAuthorities()
            .stream()
            .flatMap(a -> a.getProvidersByRealm(realm).stream())
            .map(p -> p.getResource())
            .flatMap(r -> r.getScopes().stream())
            .filter(s -> s.getScope().equals(scope))
            .findAny()
            .orElse(null);
    }

    public Scope resolveScope(String realm, String scope) throws NoSuchScopeException {
        Scope s = tryResolveScope(realm, scope);
        if (s == null) {
            throw new NoSuchScopeException();
        }

        return s;
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

        return sp.getScope();
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
        return resourceAuthorityService
            .getAuthorities()
            .stream()
            .flatMap(a -> a.getProvidersByRealm(realm).stream())
            .flatMap(p -> p.getResource().getScopes().stream())
            .collect(Collectors.toList());
    }

    public ApiScopeProvider<? extends Scope> findScopeProvider(String realm, String scopeId) {
        // ask every provider
        // TODO improve
        Scope scope = resourceAuthorityService
            .getAuthorities()
            .stream()
            .flatMap(a -> a.getProvidersByRealm(realm).stream())
            .map(p -> p.getResource())
            .flatMap(r -> r.getScopes().stream())
            .filter(s -> s.getScopeId().equals(scopeId))
            .findAny()
            .orElse(null);

        if (scope == null) {
            return null;
        }

        try {
            // direct pick
            return resourceAuthorityService
                .getAuthority(scope.getAuthority())
                .getProvider(scope.getProvider())
                .getScopeProvider(scope.getScope());
        } catch (NoSuchScopeException | NoSuchProviderException | NoSuchAuthorityException e) {
            return null;
        }
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
        return resourceAuthorityService
            .getAuthorities()
            .stream()
            .flatMap(a -> a.getProvidersByRealm(realm).stream())
            .map(p -> p.getResource())
            .filter(p -> p.getResource().equals(resource))
            .findAny()
            .orElse(null);
    }

    public ApiResource resolveResource(String realm, String resource) throws NoSuchResourceException {
        ApiResource r = tryResolveResource(realm, resource);
        if (r == null) {
            throw new NoSuchResourceException();
        }

        return r;
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

        return sp.getResource();
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
        return resourceAuthorityService
            .getAuthorities()
            .stream()
            .flatMap(a -> a.getProvidersByRealm(realm).stream())
            .map(p -> p.getResource())
            .collect(Collectors.toList());
    }

    public ApiResourceProvider<? extends ApiResource> findResourceProvider(String realm, String resourceId) {
        // by design resourceId is == providerId
        // pick first provider
        // TODO improve logic, we should resolve authority first
        return resourceAuthorityService
            .getAuthorities()
            .stream()
            .map(a -> a.findProvider(resourceId))
            .filter(p -> p != null)
            .findAny()
            .orElse(null);
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
