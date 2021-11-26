package it.smartcommunitylab.aac.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.api.scopes.ApiServicesScope;
import it.smartcommunitylab.aac.controller.BaseServicesController;

@RestController
@RequestMapping("api")
public class ApiServicesController extends BaseServicesController {
    /*
     * API controller requires a specific scope.
     * 
     * User permissions are handled at manager level.
     */
    private final static String AUTHORITY = "SCOPE_" + ApiServicesScope.SCOPE;

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }
}