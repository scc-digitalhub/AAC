package it.smartcommunitylab.aac.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.ApiRolesScope;
import it.smartcommunitylab.aac.roles.BaseRealmRolesController;

@RestController
@ApiSecurityTag(ApiRolesScope.SCOPE)
@Tag(name = "Roles", description = "Manage realm roles and roles membership")
@RequestMapping(value = "api", consumes = { MediaType.APPLICATION_JSON_VALUE,
        SystemKeys.MEDIA_TYPE_XYAML_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE,
                SystemKeys.MEDIA_TYPE_XYAML_VALUE })
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