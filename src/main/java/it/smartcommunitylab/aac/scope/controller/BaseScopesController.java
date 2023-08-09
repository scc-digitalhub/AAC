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

package it.smartcommunitylab.aac.scope.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.core.ScopeManager;
import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.scope.Scope;
import java.util.Collection;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/*
 * Base controller for scopes
 */

@PreAuthorize("hasAuthority(this.authority)")
public class BaseScopesController implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected ScopeManager scopeManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(scopeManager, "scope manager is required");
    }

    @Autowired
    public void setScopeManager(ScopeManager scopeManager) {
        this.scopeManager = scopeManager;
    }

    public String getAuthority() {
        return Config.R_USER;
    }

    @GetMapping("/scopes/{realm}")
    @Operation(summary = "Get scopes for the given realm")
    public Collection<Scope> listScopes(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm
    ) throws NoSuchRealmException {
        logger.debug("list scopes");
        return scopeManager.listScopes();
    }

    @GetMapping("/scopes/{realm}/{scope}")
    @Operation(summary = "Get a specific scope for the given realm")
    public Scope getScope(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope
    ) throws NoSuchRealmException, NoSuchScopeException {
        logger.debug("get scope {}", StringUtils.trimAllWhitespace(scope));

        return scopeManager.getScope(scope);
    }

    @GetMapping("/resources/{realm}")
    @Operation(summary = "List resources for the given realm")
    public Collection<Resource> listResources(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm
    ) throws NoSuchRealmException {
        logger.debug("list resources");

        return scopeManager.listResources();
    }

    @GetMapping("/resources/{realm}/{resourceId}")
    @Operation(summary = "Get a resource with all its scopes for the given realm")
    public Resource listResources(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String resourceId
    ) throws NoSuchRealmException, NoSuchResourceException {
        logger.debug("get resource {}", StringUtils.trimAllWhitespace(resourceId));

        return scopeManager.getResource(resourceId);
    }
}
