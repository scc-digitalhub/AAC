package it.smartcommunitylab.aac.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.ApiGroupsScope;
import it.smartcommunitylab.aac.groups.BaseGroupController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ApiSecurityTag(ApiGroupsScope.SCOPE)
@Tag(name = "Groups", description = "Manage realm groups and group membership")
@RequestMapping(
    value = "api",
    consumes = { MediaType.APPLICATION_JSON_VALUE, SystemKeys.MEDIA_TYPE_XYAML_VALUE },
    produces = { MediaType.APPLICATION_JSON_VALUE, SystemKeys.MEDIA_TYPE_XYAML_VALUE }
)
public class ApiGroupsController extends BaseGroupController {

    /*
     * API controller requires a specific scope.
     *
     * User permissions are handled at manager level.
     */
    private static final String AUTHORITY = "SCOPE_" + ApiGroupsScope.SCOPE;

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }
}
