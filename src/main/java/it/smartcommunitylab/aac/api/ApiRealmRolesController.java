package it.smartcommunitylab.aac.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.api.scopes.ApiRolesScope;
import it.smartcommunitylab.aac.roles.BaseRealmRolesController;

@RestController
@RequestMapping("api")
public class ApiRealmRolesController extends BaseRealmRolesController {
    /*
     * API controller requires a specific scope.
     * 
     * User permissions are handled at manager level.
     */
    private final static String AUTHORITY = "SCOPE_" + ApiRolesScope.SCOPE;

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }
}