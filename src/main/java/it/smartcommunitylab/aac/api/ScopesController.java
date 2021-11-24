package it.smartcommunitylab.aac.api;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.core.ScopeManager;
import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.scope.Scope;

@RestController
@RequestMapping("api")
public class ScopesController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ScopeManager scopeManager;

    // TODO evaluate restrict api to {realm} context, enforcing namespace
    // how do we handle permissions??

    @GetMapping("/scope")
    public Collection<Scope> listScopes() {
        logger.debug("list scopes");
        return scopeManager.listScopes();
    }

    @GetMapping("/scope/{scope}")
    public Scope getScope(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope)
            throws NoSuchScopeException {
        logger.debug("get scope {}",
                StringUtils.trimAllWhitespace(scope));

        return scopeManager.getScope(scope);
    }

    @GetMapping("/resources")
    public Collection<Resource> listResources() {
        logger.debug("list resources");

        return scopeManager.listResources();
    }

    @GetMapping("/resources/{resourceId}")
    public Resource listResources(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String resourceId)
            throws NoSuchResourceException {
        logger.debug("get resource {}",
                StringUtils.trimAllWhitespace(resourceId));

        return scopeManager.getResource(resourceId);
    }

}
