package it.smartcommunitylab.aac.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.ScopeProvider;

public class ServiceScopeProvider implements ScopeProvider {

    private final Service service;
    private Resource resource;
    private Map<String, ScopeApprover> approvers = new HashMap<>();

    public ServiceScopeProvider(Service service) {
        Assert.notNull(service, "services is required");
        this.service = service;
        build();
    }

    private void build() {
        resource = new Resource(service.getNamespace());
        resource.setName(service.getName());
        resource.setDescription(service.getDescription());
        
        //add realm to resource
        resource.setRealm(service.getRealm());

        // build scopes
        // include introspect client in audience
        Set<String> audience = new HashSet<>();
        audience.add(service.getNamespace());
        if (service.getClients() != null) {
            service.getClients().forEach(client -> {
                if (SystemKeys.SERVICE_CLIENT_TYPE_INTROSPECT.equals(client.getType())) {
                    audience.add(client.getClientId());
                }
            });
        }
        List<Scope> scopes = service.getScopes().stream()
                .map(s -> {
                    s.setAudience(audience);
                    return s;
                })
                .collect(Collectors.toList());
        resource.setScopes(scopes);
    }

    @Override
    public String getResourceId() {
        return service.getNamespace();
    }

    @Override
    public Collection<Scope> getScopes() {
        return new ArrayList<>(service.getScopes());
    }

    @Override
    public ScopeApprover getApprover(String scope) {
        if (!listScope().contains(scope)) {
            throw new IllegalArgumentException("invalid scope");
        }

        // we return null if no approver is defined
        return approvers.get(scope);

    }

    public void addApprover(String scope, ScopeApprover approver) {
        if (!listScope().contains(scope)) {
            throw new IllegalArgumentException("invalid scope");
        }

        approvers.put(scope, approver);
    }

    private Set<String> listScope() {
        return service.getScopes().stream().map(s -> s.getScope()).collect(Collectors.toSet());
    }

    @Override
    public Resource getResource() {
        return resource;
    }

}
