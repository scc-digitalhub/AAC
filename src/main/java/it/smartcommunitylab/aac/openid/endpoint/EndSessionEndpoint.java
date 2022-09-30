package it.smartcommunitylab.aac.openid.endpoint;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.ExtendedLogoutSuccessHandler;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.jwt.assertion.SelfAssertionValidator;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;

/*
 * https://openid.net/specs/openid-connect-rpinitiated-1_0.html
 */

@Controller
@Tag(name = "OpenID Connect Session Management")
public class EndSessionEndpoint {

    public static final String END_SESSION_URL = "/endsession";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String CLIENT_KEY = "clientId";
    private static final String STATE_KEY = "state";
    private static final String REDIRECT_URI_KEY = "redirectUri";

    // TODO
//    @Autowired
//    PersistentTokenBasedRememberMeServices rememberMeServices;

    @Autowired
    private SelfAssertionValidator validator;

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private LogoutHandler logoutHandler;

    @Autowired
    private LogoutSuccessHandler logoutSuccessHandler;

    @Autowired
    private OAuth2ClientDetailsService clientDetailsService;

    @Operation(summary = "Logout with user confirmation")
    @RequestMapping(value = END_SESSION_URL, method = RequestMethod.GET)
    public String endSession(
            @RequestParam(value = "id_token_hint", required = false) @Pattern(regexp = SystemKeys.URI_PATTERN) String idTokenHint,
            @RequestParam(value = "post_logout_redirect_uri", required = false) @Pattern(regexp = SystemKeys.URI_PATTERN) String postLogoutRedirectUri,
            @RequestParam(value = STATE_KEY, required = false) @Pattern(regexp = SystemKeys.SPECIAL_PATTERN) String state,
            HttpServletRequest request, HttpServletResponse response,
            HttpSession session, Locale locale,
            Authentication auth, Model model) throws IOException, ServletException, NoSuchRealmException {

        // get userAuth
        UserAuthentication userAuth = authHelper.getUserAuthentication();

        if (StringUtils.hasText(postLogoutRedirectUri)) {
            session.setAttribute(REDIRECT_URI_KEY, postLogoutRedirectUri);
        }

        if (StringUtils.hasText(state)) {
            session.setAttribute(STATE_KEY, state);
        }

        // parse the ID token hint to see if it's valid
        if (StringUtils.hasText(idTokenHint)) {

            try {
                JWT idToken = JWTParser.parse(idTokenHint);

                if (validator.isValid(idToken)) {
                    // we issued this ID token, figure out who it's for
                    JWTClaimsSet idTokenClaims = idToken.getJWTClaimsSet();

                    String clientId = (String) idTokenClaims.getClaim("azp");
                    if (clientId == null) {
                        clientId = idTokenClaims.getAudience().get(0);
                    }

                    if (!StringUtils.hasText(clientId)) {
                        throw new InvalidClientException("client id not found");
                    }

                    OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

                    // validate redirect
                    if (StringUtils.hasText(postLogoutRedirectUri)) {
                        if (!clientDetails.getRegisteredRedirectUri().contains(postLogoutRedirectUri)) {
                            throw new IllegalArgumentException("invalid redirect uri");
                        }
                    }

                    // save a reference in the session for us to pick up later
                    // session.setAttribute("endSession_idTokenHint_claims", idTokenClaims);
                    session.setAttribute(CLIENT_KEY, clientId);

                    // add to model for UI
                    model.addAttribute("client", clientDetails);

                } else {
                    // not a valid token, drop request
                    throw new IllegalArgumentException("invalid id_token");
                }
            } catch (ParseException e) {
                // it's not a valid ID token, drop request
                logger.debug("Invalid id token hint", e);
                throw new IllegalArgumentException("invalid id_token");
            } catch (InvalidClientException | ClientRegistrationException e) {
                // couldn't find the client, drop request
                logger.debug("Invalid client", e);
                throw new IllegalArgumentException("invalid id_token");
            }

        }

        // are we logged in or not?
        if (userAuth == null) {
            // we're not logged in anyway, process the final redirect bits if needed
            processLogout(Optional.of("true"), request, response, session, auth, model);
            return null;
        } else {
            // we are logged in, need to prompt the user before we log out
            // display the log out confirmation page
            UserDetails userDetails = userAuth.getUser();
            // add user info
            String userName = StringUtils.hasText(userDetails.getUsername()) ? userDetails.getUsername()
                    : userDetails.getSubjectId();
//            String fullName = userDetails.getFullName();
            model.addAttribute("fullname", userName);
            model.addAttribute("username", userName);

            // load realm props
            String realm = userAuth.getRealm();
            model.addAttribute("realm", realm);
            model.addAttribute("displayName", realm);

            // add form action
            model.addAttribute("formAction", END_SESSION_URL);
            return "endsession";
        }
    }

    @Hidden
    @RequestMapping(value = END_SESSION_URL, method = RequestMethod.POST)
    public void processLogout(@RequestParam(value = "approve", required = false) Optional<String> approve,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session, Authentication auth,
            Model model) throws IOException, ServletException {

        String redirectUri = (String) session.getAttribute(REDIRECT_URI_KEY);
        String state = (String) session.getAttribute(STATE_KEY);
        String clientId = (String) session.getAttribute(CLIENT_KEY);

        boolean approved = approve.isPresent() ? true : false;

        // build default redirect base
        URI requestUri = UriComponentsBuilder.fromUriString(request.getRequestURL().toString()).build().toUri();
        UriComponentsBuilder baseUri = UriComponentsBuilder.newInstance();
        baseUri.scheme(requestUri.getScheme()).host(requestUri.getHost()).port(requestUri.getPort());
        String redirect = null;

        // if we have a client AND the client has post-logout redirect URIs
        // registered AND the URI given is in that list, then use it
        OAuth2ClientDetails clientDetails = null;
        if (StringUtils.hasText(clientId)) {
            try {
                clientDetails = clientDetailsService.loadClientByClientId(clientId);
            } catch (InvalidClientException | ClientRegistrationException e) {
                // couldn't find the client, drop request
                logger.debug("Invalid client", e);
                throw new IllegalArgumentException("invalid id_token");
            }
        }

        if (StringUtils.hasText(redirectUri) && clientDetails != null) {
            if (clientDetails.getRegisteredRedirectUri().contains(redirectUri)) {
                // TODO: future, add the redirect URI to the model for the display page for an
                // interstitial
                // m.addAttribute("redirectUri", postLogoutRedirectUri);

                // add state param from session
                UriComponents uri = UriComponentsBuilder.fromUriString(redirectUri).queryParam("state", state).build();
                redirect = uri.toUriString();
            }
        }

        if (approved && auth != null) {
//                // leverage rememberme service to clear cookie
//                rememberMeServices.logout(request, response, auth);
            // logout
            logoutHandler.logout(request, response, auth);

            // set redirect, if null will led to login
            request.setAttribute(ExtendedLogoutSuccessHandler.REDIRECT_ATTRIBUTE, redirect);

            // let logoutSuccess process request
            logoutSuccessHandler.onLogoutSuccess(request, response, auth);

            // TODO: hook into other logout post-processing
        } else {

            // if the user didn't approve, don't log out but
            // redirect as needed

            if (redirect == null) {
                redirect = baseUri.path("").toUriString();
            }

            response.sendRedirect(redirect);

        }
    }

}