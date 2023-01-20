package it.smartcommunitylab.aac.api.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.ApiProviderScope;
import it.smartcommunitylab.aac.controller.BaseAttributeProviderController;

@RestController
@ApiSecurityTag(ApiProviderScope.SCOPE)
@Tag(name = "Attribute Providers", description = "Manage realm attribute providers and their configuration")
@RequestMapping(value = "api", consumes = { MediaType.APPLICATION_JSON_VALUE,
        SystemKeys.MEDIA_TYPE_XYAML_VALUE }, produces = {
                MediaType.APPLICATION_JSON_VALUE, SystemKeys.MEDIA_TYPE_XYAML_VALUE })
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
