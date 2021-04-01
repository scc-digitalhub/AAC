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

    @GetMapping("")
    public Collection<Scope> listScopes() {
        return scopeManager.listScopes();
    }

    @GetMapping("{scope}")
    public Scope getScope(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope) throws NoSuchScopeException {
        return scopeManager.getScope(scope);
    }

    @PostMapping()
    public Scope addScope(
            @RequestBody @Valid Scope s) {
        return scopeManager.addScope(s);
    }

    @PutMapping("{scope}")
    public Scope updateScope(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope,
            @RequestBody @Valid Scope s) throws NoSuchScopeException {
        return scopeManager.updateScope(scope, s);
    }

    @DeleteMapping("{scope}")
    public void deleteScope(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope) {
        scopeManager.deleteScope(scope);
        ;
    }

}
