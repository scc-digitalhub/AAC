package it.smartcommunitylab.aac.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.api.scopes.ApiAttributesScope;
import it.smartcommunitylab.aac.attributes.BaseAttributeSetsController;

@RestController
@RequestMapping("api")
@PreAuthorize("hasAuthority('SCOPE_" + ApiAttributesScope.SCOPE + "')")
public class ApiAttributesController extends BaseAttributeSetsController {

    private final static String AUTHORITY = "SCOPE_" + ApiAttributesScope.SCOPE;

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }

}
