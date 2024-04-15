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

package it.smartcommunitylab.aac.core;

import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeRegistry;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

//TODO evaluate split scopes *per realm*
//TODO permissions
@Service
public class ScopeManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ScopeRegistry scopeRegistry;

    //
    //    @Autowired
    //    private SearchableApprovalStore approvalStore;

    public Collection<Scope> listScopes() {
        logger.debug("list scopes");

        // fetch from registry
        return scopeRegistry.listScopes();
    }

    @Deprecated
    public Scope findScope(String scope) {
        logger.debug("find scope {}", StringUtils.trimAllWhitespace(scope));

        // from registry
        return scopeRegistry.findScope(scope);
    }

    public Scope getScope(String scope) throws NoSuchScopeException {
        logger.debug("get scope {}", StringUtils.trimAllWhitespace(scope));

        // from registry
        return scopeRegistry.getScope(scope);
    }

    public Collection<Resource> listResources() {
        logger.debug("list resources");

        // fetch from registry
        return scopeRegistry.listResources();
    }

    @Deprecated
    public Resource findResource(String resourceId) {
        logger.debug("find resource {}", String.valueOf(resourceId));

        // from registry
        return scopeRegistry.findResource(resourceId);
    }

    public Resource getResource(String resourceId) throws NoSuchResourceException {
        logger.debug("get resource {}", StringUtils.trimAllWhitespace(resourceId));

        // from registry
        return scopeRegistry.getResource(resourceId);
    }
}
