package it.smartcommunitylab.aac.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.api.scopes.ApiProviderScope;
import it.smartcommunitylab.aac.controller.BaseAttributeProviderController;

@RestController
@RequestMapping("api")
public class ApiAttributeProviderController extends BaseAttributeProviderController {
    /*
     * API controller requires a specific scope.
     * 
     * User permissions are handled at manager level.
     */
    private final static String AUTHORITY = "SCOPE_" + ApiProviderScope.SCOPE;

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }
}
