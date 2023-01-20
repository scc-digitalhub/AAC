package it.smartcommunitylab.aac.api.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.ApiRealmScope;
import it.smartcommunitylab.aac.templates.BaseTemplatesController;

@RestController
@ApiSecurityTag(ApiRealmScope.SCOPE)
@Tag(name = "Templates", description = "Manage realm templates")
@RequestMapping(value = "api", consumes = { MediaType.APPLICATION_JSON_VALUE,
        SystemKeys.MEDIA_TYPE_XYAML_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE,
                SystemKeys.MEDIA_TYPE_XYAML_VALUE })
public class ApiTemplatesController extends BaseTemplatesController {
    /*
     * API controller requires a specific scope.
     * 
     * User permissions are handled at manager level.
     */
    private final static String AUTHORITY = "SCOPE_" + ApiRealmScope.SCOPE;

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }

}
