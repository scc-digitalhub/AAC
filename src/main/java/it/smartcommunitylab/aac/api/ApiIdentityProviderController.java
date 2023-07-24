package it.smartcommunitylab.aac.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.ApiProviderScope;
import it.smartcommunitylab.aac.controller.BaseIdentityProviderController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ApiSecurityTag(ApiProviderScope.SCOPE)
@Tag(name = "Identity Providers", description = "Manage realm identity providers and their configuration")
@RequestMapping(
    value = "api",
    consumes = { MediaType.APPLICATION_JSON_VALUE, SystemKeys.MEDIA_TYPE_XYAML_VALUE },
    produces = { MediaType.APPLICATION_JSON_VALUE, SystemKeys.MEDIA_TYPE_XYAML_VALUE }
)
public class ApiIdentityProviderController extends BaseIdentityProviderController {

    /*
     * API controller requires a specific scope.
     *
     * User permissions are handled at manager level.
     */
    private static final String AUTHORITY = "SCOPE_" + ApiProviderScope.SCOPE;

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }
}
