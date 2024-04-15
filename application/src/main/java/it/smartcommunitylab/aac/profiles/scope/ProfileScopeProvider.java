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

package it.smartcommunitylab.aac.profiles.scope;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.profiles.claims.ProfileClaimsSet;
import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.ScopeProvider;
import it.smartcommunitylab.aac.scope.WhitelistScopeApprover;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/*
 * A simple scope provider which return profile scopes
 */
@Component
public class ProfileScopeProvider implements ScopeProvider {

    private final ProfileResource resource = new ProfileResource();
    public static final Set<Scope> scopes;
    private static final Map<String, WhitelistScopeApprover> approvers;

    static {
        Set<Scope> s = new HashSet<>();
        s.add(new BasicProfileScope());
        s.add(new AccountProfileScope());

        scopes = Collections.unmodifiableSet(s);

        Map<String, WhitelistScopeApprover> a = new HashMap<>();
        for (Scope sc : scopes) {
            a.put(sc.getScope(), new WhitelistScopeApprover(null, sc.getResourceId(), sc.getScope()));
        }

        approvers = a;
    }

    private final AttributeService attributeService;

    // loading cache for set profile approvers
    private final LoadingCache<String, WhitelistScopeApprover> setApprovers = CacheBuilder
        .newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
        .maximumSize(100)
        .build(
            new CacheLoader<String, WhitelistScopeApprover>() {
                @Override
                public WhitelistScopeApprover load(final String scope) throws Exception {
                    return new WhitelistScopeApprover(null, resource.getResourceId(), scope);
                }
            }
        );

    public ProfileScopeProvider(AttributeService attributeService) {
        Assert.notNull(attributeService, "attribute service is required");
        this.attributeService = attributeService;
    }

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID;
    }

    @Override
    public Resource getResource() {
        resource.setScopes(getScopes());
        return resource;
    }

    @Override
    public Collection<Scope> getScopes() {
        Set<Scope> res = new HashSet<>();
        res.addAll(scopes);

        attributeService
            .listAttributeSets()
            .stream()
            .forEach(a -> {
                res.add(new CustomProfileScope(a.getIdentifier()));
            });

        return res;
    }

    @Override
    public ScopeApprover getApprover(String scope) {
        if (approvers.containsKey(scope)) {
            return approvers.get(scope);
        }

        // check if scope is a set id
        String id = extractId(scope);
        AttributeSet set = attributeService.findAttributeSet(id);
        if (set != null) {
            try {
                return setApprovers.get(scope);
            } catch (ExecutionException e) {
                return null;
            }
        }

        return null;
    }

    private String extractId(String scope) {
        if (scope.startsWith("profile.") && scope.endsWith(".me")) {
            return scope.substring(8, scope.length() - 3);
        }
        return scope;
    }
}
