package it.smartcommunitylab.aac.api;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.core.ScopeManager;
import it.smartcommunitylab.aac.scope.Scope;

@RestController
@RequestMapping("api/scope")
public class ScopesController {

    @Autowired
    private ScopeManager scopeManager;

    // TODO evaluate restrict api to {realm} context, enforcing namespace
    // how do we handle permissions??

    @GetMapping("")
    public Collection<Scope> listScopes() {
        return scopeManager.listScopes();
    }

    @GetMapping("{scope}")
    public Scope getScope(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope) throws NoSuchScopeException {
        return scopeManager.getScope(scope);
    }

}
