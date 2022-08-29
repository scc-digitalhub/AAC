package it.smartcommunitylab.aac.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.ApiClientAppScope;
import it.smartcommunitylab.aac.controller.BaseClientAppController;

/*
 * API controller for clientApp
 */
@RestController
@Tag(name = "Client apps", description = "Manage client applications and their configuration")
@RequestMapping(value = "api", consumes = { MediaType.APPLICATION_JSON_VALUE,
        SystemKeys.MEDIA_TYPE_XYAML_VALUE }, produces = {
                MediaType.APPLICATION_JSON_VALUE, SystemKeys.MEDIA_TYPE_XYAML_VALUE })
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
