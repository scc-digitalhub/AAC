package it.smartcommunitylab.aac.api.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.ApiServicesScope;
import it.smartcommunitylab.aac.services.BaseServicesController;

@RestController
@ApiSecurityTag(ApiServicesScope.SCOPE)
@Tag(name = "Custom services", description = "Manage custom services, scopes and claims")
@RequestMapping(value = "api", consumes = { MediaType.APPLICATION_JSON_VALUE,
        SystemKeys.MEDIA_TYPE_XYAML_VALUE }, produces = {
                MediaType.APPLICATION_JSON_VALUE, SystemKeys.MEDIA_TYPE_XYAML_VALUE })
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