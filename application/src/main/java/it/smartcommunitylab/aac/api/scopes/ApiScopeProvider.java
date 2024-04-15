/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.scope.AuthorityScopeApprover;
import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.ScopeProvider;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ApiScopeProvider implements ScopeProvider {

    private static final ApiResource resource = new ApiResource();
    private static final Map<String, ApiScope> scopes;
    private static final Map<String, ScopeApprover> approvers;

    static {
        Map<String, ApiScope> s = new HashMap<>();
        s.put(ApiAuditScope.SCOPE, new ApiAuditScope());
        s.put(ApiClientAppScope.SCOPE, new ApiClientAppScope());
        s.put(ApiProviderScope.SCOPE, new ApiProviderScope());
        s.put(ApiServicesScope.SCOPE, new ApiServicesScope());
        s.put(ApiUsersScope.SCOPE, new ApiUsersScope());
        s.put(ApiAttributesScope.SCOPE, new ApiAttributesScope());
        s.put(ApiScopesScope.SCOPE, new ApiScopesScope());
        s.put(ApiRealmScope.SCOPE, new ApiRealmScope());
        s.put(ApiRolesScope.SCOPE, new ApiRolesScope());
        s.put(ApiGroupsScope.SCOPE, new ApiGroupsScope());

        scopes = Collections.unmodifiableMap(s);
        resource.setScopes(Collections.unmodifiableSet(new HashSet<>(scopes.values())));

        // map all to realm role,
        // will work only for realm matching requests thanks to
        // user translation, ie a client can ask for a user to
        // consent scopes for managing the client's realm, if the user has those
        // authorities. We don't support a global client, except when authorities by the
        // super admin
        Map<String, ScopeApprover> a = new HashMap<>();
        for (ApiScope sc : s.values()) {
            AuthorityScopeApprover sa = new AuthorityScopeApprover(null, sc.getResourceId(), sc.getScope());
            sa.setAuthorities(sc.getAuthorities());
            a.put(sc.getScope(), sa);
        }

        approvers = a;
    }

    @Override
    public String getResourceId() {
        return ApiResource.RESOURCE_ID;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public Collection<Scope> getScopes() {
        return resource.getScopes();
    }

    @Override
    public ScopeApprover getApprover(String scope) {
        return approvers.get(scope);
    }

    public ApiScope getScope(String scope) {
        return scopes.get(scope);
    }
}
