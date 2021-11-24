package it.smartcommunitylab.aac.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.api.scopes.ApiAttributesScope;
import it.smartcommunitylab.aac.attributes.BaseAttributeSetsController;

@RestController
@RequestMapping("api")
public class ApiAttributesController extends BaseAttributeSetsController {

    /*
     * API controller requires a specific scope.
     * 
     * User permissions are handled at manager level.
     */
    private final static String AUTHORITY = "SCOPE_" + ApiAttributesScope.SCOPE;

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }

}
