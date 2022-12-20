package it.smartcommunitylab.aac.profiles.scope;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.AbstractInternalApiScope;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.roles.scopes.RolesResource;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.ScopeProvider;
import it.smartcommunitylab.aac.scope.approver.SubjectTypeScopeApprover;
import it.smartcommunitylab.aac.scope.approver.WhitelistScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractScopeProvider;
import it.smartcommunitylab.aac.scope.model.ApiResource;
import it.smartcommunitylab.aac.scope.model.Scope;

/*
 * A simple scope provider which return profile scopes
 */

public class OpenIdProfileScopeProvider extends AbstractScopeProvider<AbstractInternalApiScope> {

    private final Map<String, AbstractInternalApiScope> scopes;
    private final Map<String, SubjectTypeScopeApprover<AbstractInternalApiScope>> approvers;

    public OpenIdProfileScopeProvider(OpenIdUserInfoResource resource) {
        super(SystemKeys.AUTHORITY_OIDC, resource.getProvider());
        Assert.notNull(resource, "resource can not be null");

        // extract scopes
        this.scopes = resource.getScopes().stream()
                .collect(Collectors.toMap(s -> s.getScope(), s -> s));

        // init approvers map
        approvers = new HashMap<>();
    }

    @Override
    public AbstractInternalApiScope findScope(String scope) {
        return scopes.get(scope);
    }

    @Override
    public AbstractInternalApiScope getScope(String scope) throws NoSuchScopeException {
        AbstractInternalApiScope s = findScope(scope);
        if (s == null) {
            throw new NoSuchScopeException();
        }

        return s;
    }

    @Override
    public Collection<AbstractInternalApiScope> listScopes() {
        return Collections.unmodifiableCollection(scopes.values());
    }

    @Override
    public SubjectTypeScopeApprover<AbstractInternalApiScope> getScopeApprover(String scope)
            throws NoSuchScopeException {
        AbstractInternalApiScope s = getScope(scope);
        if (!approvers.containsKey(scope)) {
            // build approver based on type
            SubjectTypeScopeApprover<AbstractInternalApiScope> sa = new SubjectTypeScopeApprover<>(s);
            sa.setSubjectType(s.getSubjectType());
            approvers.put(scope, sa);
        }

        return approvers.get(scope);
    }

}
