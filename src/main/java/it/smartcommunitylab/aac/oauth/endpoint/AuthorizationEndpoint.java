package it.smartcommunitylab.aac.oauth.endpoint;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.endpoint.AbstractEndpoint;
import org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

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

    @RequestMapping(value = "/oauth/authorize")
    public ModelAndView authorize(Map<String, Object> model, @RequestParam Map<String, String> parameters,
            SessionStatus sessionStatus, Principal principal) {
        return authEndpoint.authorize(model, parameters, sessionStatus, principal);
    }

    @RequestMapping(value = "/oauth/authorize", method = RequestMethod.POST, params = OAuth2Utils.USER_OAUTH_APPROVAL)
    public View approveOrDeny(@RequestParam Map<String, String> approvalParameters, Map<String, ?> model,
            SessionStatus sessionStatus, Principal principal) {
        return authEndpoint.approveOrDeny(approvalParameters, model, sessionStatus, principal);
    }

}
