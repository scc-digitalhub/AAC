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

import it.smartcommunitylab.aac.Config;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class AdminScopeProvider implements ScopeProvider {

    private static final AdminResource resource = new AdminResource();
    private static final Map<String, Scope> scopes;
    public static final Map<String, ScopeApprover> approvers;

    static {
        Map<String, Scope> s = new HashMap<>();
        s.put(AdminRealmsScope.SCOPE, new AdminRealmsScope());

        scopes = Collections.unmodifiableMap(s);
        resource.setScopes(Collections.unmodifiableSet(new HashSet<>(scopes.values())));

        // map all to global admin role
        Map<String, ScopeApprover> a = new HashMap<>();
        for (Scope sc : s.values()) {
            AuthorityScopeApprover sa = new AuthorityScopeApprover(null, sc.getResourceId(), sc.getScope());
            // ask exact match
            sa.setGrantedAuthorities(Collections.singleton(new SimpleGrantedAuthority(Config.R_ADMIN)));
            a.put(sc.getScope(), sa);
        }

        approvers = a;
    }

    @Override
    public String getResourceId() {
        return AdminResource.RESOURCE_ID;
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
}
