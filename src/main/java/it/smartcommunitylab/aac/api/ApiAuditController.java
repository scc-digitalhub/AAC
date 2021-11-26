package it.smartcommunitylab.aac.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.api.scopes.ApiAuditScope;
import it.smartcommunitylab.aac.controller.BaseAuditController;

@RestController
@RequestMapping("api")
public class ApiAuditController extends BaseAuditController {
    /*
     * API controller requires a specific scope.
     * 
     * User permissions are handled at manager level.
     */
    private final static String AUTHORITY = "SCOPE_" + ApiAuditScope.SCOPE;

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }
}
