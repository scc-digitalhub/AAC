package it.smartcommunitylab.aac.core;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.oauth.approval.SearchableApprovalStore;
import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

@Service
public class ScopeManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ScopeRegistry scopeRegistry;

    @Autowired
    private SearchableApprovalStore approvalStore;

    public Collection<Scope> listScopes() {
        // fetch from registry
        return scopeRegistry.listScopes();
    }

    public Scope findScope(String scope) {
        // from registry
        return scopeRegistry.findScope(scope);
    }

    public Scope getScope(String scope) throws NoSuchScopeException {
        // from registry
        return scopeRegistry.getScope(scope);
    }

    public Collection<Resource> listResources() {
        // fetch from registry
        return scopeRegistry.listResources();
    }

    public Resource findResource(String resourceId) {
        // from registry
        return scopeRegistry.findResource(resourceId);
    }

    public Resource getResource(String resourceId) throws NoSuchResourceException {
        // from registry
        return scopeRegistry.getResource(resourceId);
    }

}
