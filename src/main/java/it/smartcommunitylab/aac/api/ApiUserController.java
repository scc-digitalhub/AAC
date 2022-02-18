package it.smartcommunitylab.aac.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.api.scopes.ApiUsersScope;
import it.smartcommunitylab.aac.controller.BaseUserController;

@RestController
@RequestMapping("api")
public class ApiUserController extends BaseUserController {
    /*
     * API controller requires a specific scope.
     * 
     * User permissions are handled at manager level.
     */
    private final static String AUTHORITY = "SCOPE_" + ApiUsersScope.SCOPE;

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }
}