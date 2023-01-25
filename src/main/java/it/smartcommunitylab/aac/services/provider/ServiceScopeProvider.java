package it.smartcommunitylab.aac.services.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.ScopeProvider;
import it.smartcommunitylab.aac.scope.model.ApiResource;
import it.smartcommunitylab.aac.scope.model.Scope;
import it.smartcommunitylab.aac.services.model.ApiService;

public class ServiceScopeProvider implements ScopeProvider {

    private final ApiService service;
    private ApiResource resource;
    private Map<String, ScopeApprover> approvers = new HashMap<>();

    public ServiceScopeProvider(ApiService service) {
        Assert.notNull(service, "services is required");
        this.service = service;
        build();
    }

    private void build() {
//        resource = new ApiResource(service.getNamespace());
//        resource.setName(service.getName());
//        resource.setDescription(service.getDescription());
//
//        // add realm to resource
//        resource.setRealm(service.getRealm());
//
//        // build scopes
//        Set<String> audience = new HashSet<>();
//        audience.add(service.getNamespace());
//
//        List<Scope> scopes = service.getScopes().stream()
//                .map(s -> {
//                    s.setAudience(audience);
//                    return s;
//                })
//                .collect(Collectors.toList());
//        resource.setScopes(scopes);
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
    public ApiResource getResource() {
        return resource;
    }

}
