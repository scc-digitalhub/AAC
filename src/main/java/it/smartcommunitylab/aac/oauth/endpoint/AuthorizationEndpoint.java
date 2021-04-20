package it.smartcommunitylab.aac.oauth.endpoint;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.endpoint.AbstractEndpoint;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import it.smartcommunitylab.aac.SystemKeys;

/*
 * Implementation of AuthorizationEndpoint based on spring-security-oauth2.
*/

@Controller
@SessionAttributes({ AuthorizationEndpoint.AUTHORIZATION_REQUEST_ATTR_NAME,
        AuthorizationEndpoint.ORIGINAL_AUTHORIZATION_REQUEST_ATTR_NAME })
public class AuthorizationEndpoint {
    static final String AUTHORIZATION_REQUEST_ATTR_NAME = "authorizationRequest";

    static final String ORIGINAL_AUTHORIZATION_REQUEST_ATTR_NAME = "org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint.ORIGINAL_AUTHORIZATION_REQUEST";

    @Autowired
    private org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint authEndpoint;

    @RequestMapping(value = {
            "/oauth/authorize",
            "/-/{realm}/oauth/authorize" })
    public ModelAndView authorize(Map<String, Object> model, @RequestParam Map<String, String> parameters,
            @PathVariable("realm") Optional<String> realmKey,
            SessionStatus sessionStatus, Principal principal) {
        String realm = SystemKeys.REALM_COMMON;
        if (realmKey.isPresent()) {
            realm = realmKey.get();
        }

        // build a new map with realm parameter
        Map<String, String> params = new HashMap<>();
        params.putAll(parameters);
        params.put("realm", realm);

        return authEndpoint.authorize(model, params, sessionStatus, principal);
    }

    @RequestMapping(value = {
            "/oauth/authorize",
            "/-/{realm}/oauth/authorize" }, method = RequestMethod.POST, params = OAuth2Utils.USER_OAUTH_APPROVAL)
    public View approveOrDeny(@RequestParam Map<String, String> approvalParameters, Map<String, ?> model,
            @PathVariable("realm") Optional<String> realmKey,
            SessionStatus sessionStatus, Principal principal) {
        String realm = SystemKeys.REALM_COMMON;
        if (realmKey.isPresent()) {
            realm = realmKey.get();
        }

        // build a new map with realm parameter
        Map<String, String> params = new HashMap<>();
        params.putAll(approvalParameters);
        params.put("realm", realm);

        return authEndpoint.approveOrDeny(params, model, sessionStatus, principal);
    }

}