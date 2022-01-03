package it.smartcommunitylab.aac.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.api.scopes.ApiClientAppScope;
import it.smartcommunitylab.aac.controller.BaseClientAppController;

/*
 * API controller for clientApp
 */
@RestController
@RequestMapping("api")
public class ApiClientAppController extends BaseClientAppController {
    /*
     * API controller requires a specific scope.
     * 
     * User permissions are handled at manager level.
     */
    private final static String AUTHORITY = "SCOPE_" + ApiClientAppScope.SCOPE;

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }
}
