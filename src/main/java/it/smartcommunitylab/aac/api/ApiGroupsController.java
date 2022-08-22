package it.smartcommunitylab.aac.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.api.scopes.ApiGroupsScope;
import it.smartcommunitylab.aac.groups.BaseGroupController;

@RestController
@Tag(name = "Groups", description = "Manage realm groups and group membership")
@RequestMapping(value = "api", consumes = { MediaType.APPLICATION_JSON_VALUE, "application/x-yaml" }, produces = {
        MediaType.APPLICATION_JSON_VALUE, "application/x-yaml" })
public class ApiGroupsController extends BaseGroupController {
    /*
     * API controller requires a specific scope.
     * 
     * User permissions are handled at manager level.
     */
    private final static String AUTHORITY = "SCOPE_" + ApiGroupsScope.SCOPE;

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }
}