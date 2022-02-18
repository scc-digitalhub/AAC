package it.smartcommunitylab.aac.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.api.scopes.ApiScopesScope;
import it.smartcommunitylab.aac.controller.BaseScopesController;

@RestController
@RequestMapping("api")
public class ApiScopesController extends BaseScopesController {
    /*
     * API controller requires a specific scope.
     * 
     * User permissions are handled at manager level.
     */
    private final static String AUTHORITY = "SCOPE_" + ApiScopesScope.SCOPE;

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }
}