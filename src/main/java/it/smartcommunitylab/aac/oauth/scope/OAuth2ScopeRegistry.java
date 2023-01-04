package it.smartcommunitylab.aac.oauth.scope;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.oauth.model.Resource;
import it.smartcommunitylab.aac.oauth.model.Scope;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

@Service
public class OAuth2ScopeRegistry {

    // aac services
    private final ScopeRegistry registry;

    public OAuth2ScopeRegistry(ScopeRegistry registry) {
        Assert.notNull(registry, "registry can not be null");
        this.registry = registry;
    }

    // TODO evaluate how to resolve realm, via client or via realm param?
    // or instantiate a single registry per realm, as a provider?

    public Scope lookupScope(String clientId, String scope) {

    }

    public Resource lookupResource(String clientId, String scope) {

    }

}
