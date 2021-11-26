package it.smartcommunitylab.aac.controller;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.core.ScopeManager;
import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.scope.Scope;

/*
 * Base controller for scopes
 */

@PreAuthorize("hasAuthority(this.authority)")
public class BaseScopesController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ScopeManager scopeManager;

    public String getAuthority() {
        return Config.R_USER;
    }

    @GetMapping("/scope/{realm}")
    public Collection<Scope> listScopes(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        logger.debug("list scopes");
        return scopeManager.listScopes();
    }

    @GetMapping("/scope/{realm}/{scope}")
    public Scope getScope(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope)
            throws NoSuchRealmException, NoSuchScopeException {
        logger.debug("get scope {}",
                StringUtils.trimAllWhitespace(scope));

        return scopeManager.getScope(scope);
    }

    @GetMapping("/resources/{realm}")
    public Collection<Resource> listResources(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        logger.debug("list resources");

        return scopeManager.listResources();
    }

    @GetMapping("/resources/{realm}/{resourceId}")
    public Resource listResources(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String resourceId)
            throws NoSuchRealmException, NoSuchResourceException {
        logger.debug("get resource {}",
                StringUtils.trimAllWhitespace(resourceId));

        return scopeManager.getResource(resourceId);
    }

}
