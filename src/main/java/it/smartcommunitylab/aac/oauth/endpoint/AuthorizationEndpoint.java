package it.smartcommunitylab.aac.oauth.endpoint;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.common.exceptions.BadClientCredentialsException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.endpoint.AbstractEndpoint;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.oauth.event.OAuth2EventPublisher;

/*
 * Implementation of AuthorizationEndpoint based on spring-security-oauth2.
*/

@Controller
@SessionAttributes({ AuthorizationEndpoint.AUTHORIZATION_REQUEST_ATTR_NAME,
        AuthorizationEndpoint.ORIGINAL_AUTHORIZATION_REQUEST_ATTR_NAME })
public class AuthorizationEndpoint {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String AUTHORIZATION_URL = "/oauth/authorize";

    static final String AUTHORIZATION_REQUEST_ATTR_NAME = "authorizationRequest";

    static final String ORIGINAL_AUTHORIZATION_REQUEST_ATTR_NAME = "org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint.ORIGINAL_AUTHORIZATION_REQUEST";

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint authEndpoint;

    @Autowired
    private OAuth2EventPublisher eventPublisher;

    // TODO support POST requests for auth endpoint as per
    // https://datatracker.ietf.org/doc/html/rfc6749#section-3.1
    @RequestMapping(value = {
            AUTHORIZATION_URL,
            "/-/{realm}" + AUTHORIZATION_URL })
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

        // ensure we have a full user auth
        UserAuthenticationToken userAuth = authHelper.getUserAuthentication();
        if (userAuth == null) {
            throw new InsufficientAuthenticationException("user must be authenticated");
        }

        return authEndpoint.authorize(model, params, sessionStatus, principal);
    }

    // TODO move to /approve
    @RequestMapping(value = {
            AUTHORIZATION_URL,
            "/-/{realm}" + AUTHORIZATION_URL }, method = RequestMethod.POST, params = OAuth2Utils.USER_OAUTH_APPROVAL)
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

    @ExceptionHandler(ClientRegistrationException.class)
    public ModelAndView handleClientRegistrationException(ClientRegistrationException e, ServletWebRequest webRequest)
            throws Exception {
//        if (eventPublisher != null) {
//            eventPublisher.publishOAuth2Exception(new BadClientCredentialsException(), authHelper.getAuthentication());
//        }

        return authEndpoint.handleClientRegistrationException(e, webRequest);
    }

    @ExceptionHandler(OAuth2Exception.class)
    public ModelAndView handleOAuth2Exception(OAuth2Exception e, ServletWebRequest webRequest) throws Exception {
        logger.error("error requesting authorization: " + e.getMessage());
        logger.trace(e.getStackTrace().toString());
//        if (eventPublisher != null) {
//            eventPublisher.publishOAuth2Exception(e, null);
//        }
        return authEndpoint.handleOAuth2Exception(e, webRequest);
    }

}
