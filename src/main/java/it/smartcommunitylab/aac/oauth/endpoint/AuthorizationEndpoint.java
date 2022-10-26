package it.smartcommunitylab.aac.oauth.endpoint;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.common.exceptions.RedirectMismatchException;
import org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;
import org.springframework.security.oauth2.common.exceptions.UserDeniedAuthorizationException;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.common.ServerErrorException;
import it.smartcommunitylab.aac.oauth.model.AuthorizationResponse;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.model.PromptMode;
import it.smartcommunitylab.aac.oauth.model.ResponseMode;
import it.smartcommunitylab.aac.oauth.request.OAuth2AuthorizationRequestFactory;
import it.smartcommunitylab.aac.oauth.request.OAuth2AuthorizationRequestValidator;
import it.smartcommunitylab.aac.oauth.request.OAuth2TokenRequestFactory;
import it.smartcommunitylab.aac.oauth.request.OAuth2TokenRequestValidator;
import it.smartcommunitylab.aac.oauth.request.TokenRequest;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.store.AuthorizationRequestStore;
import it.smartcommunitylab.aac.openid.common.IdToken;
import it.smartcommunitylab.aac.openid.token.IdTokenServices;

/*
 * OAuth2/OIDC 
 * supports
 * https://datatracker.ietf.org/doc/html/rfc6749
 * https://datatracker.ietf.org/doc/html/rfc7636
 * https://openid.net/specs/openid-connect-core-1_0.html
 * https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html
 * https://datatracker.ietf.org/doc/html/draft-ietf-oauth-iss-auth-resp-01
 * 
*/

@Controller
public class AuthorizationEndpoint implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String AUTHORIZATION_URL = "/oauth/authorize";
    public static final String AUTHORIZED_URL = "/oauth/authorized";
    public static final String FORM_POST_URL = "/oauth/authorized_post";

    public static final String AUTHORIZATION_REQUEST_ATTR_NAME = "authorizationRequest";
    public static final String ORIGINAL_AUTHORIZATION_REQUEST_ATTR_NAME = "org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint.ORIGINAL_AUTHORIZATION_REQUEST";

    private static final String userApprovalView = "forward:" + UserApprovalEndpoint.ACCESS_CONFIRMATION_URL;
    private static final String errorView = "forward:" + ErrorEndpoint.ERROR_URL;
    private static final String responseView = "forward:" + AUTHORIZED_URL;
    private static final String formView = "forward:" + FORM_POST_URL;
//    private static final String formPage = "oauth_form_post.html";
    private static final String formPage = FormPostView.VIEWNAME;

    @Value("${jwt.issuer}")
    private String issuer;

    @Autowired
    private OAuth2AuthorizationRequestFactory oauth2AuthorizationRequestFactory;

    @Autowired
    private OAuth2AuthorizationRequestValidator oauth2AuthorizationRequestValidator;

    @Autowired
    private OAuth2TokenRequestFactory oauth2TokenRequestFactory;

    @Autowired
    private OAuth2TokenRequestValidator oauth2TokenRequestValidator;

    @Autowired
    private AuthorizationRequestStore oauth2AuthorizationRequestRepository;

    @Autowired
    private OAuth2ClientDetailsService oauth2ClientDetailsService;

    @Autowired
    private RedirectResolver redirectResolver;

    @Autowired
    private AuthorizationCodeServices authorizationCodeServices;

    @Autowired
    private UserApprovalHandler userApprovalHandler;

    @Autowired
    private TokenGranter tokenGranter;

    @Autowired
    private IdTokenServices idTokenServices;

    @Autowired
    private UserService userService;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(oauth2ClientDetailsService, "oauth2 client service is required");
        Assert.notNull(oauth2AuthorizationRequestFactory, "authorization request factory is required");
        Assert.notNull(oauth2AuthorizationRequestValidator, "authorization request validator is required");
        Assert.notNull(oauth2AuthorizationRequestRepository, "request repository is required");
        Assert.notNull(oauth2TokenRequestFactory, "token request factory is required");
        Assert.notNull(oauth2TokenRequestValidator, "token request validator is required");
        Assert.notNull(userApprovalHandler, "user approval handler is required");
    }

//   
//    @Autowired
//    private OAuth2EventPublisher eventPublisher;

//    @Operation(summary = "Authorize request", parameters = { @io.swagger.v3.oas.annotations.Parameter(content = {
//            @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = AuthorizationRequest.class)) }) }, responses = {
//                    @io.swagger.v3.oas.annotations.responses.ApiResponse(content = {
//                            @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = AuthorizationResponse.class)) }) })
    @RequestMapping(value = {
            AUTHORIZATION_URL,
            "/-/{realm}" + AUTHORIZATION_URL }, method = {
                    RequestMethod.GET, RequestMethod.POST
            })
    public ModelAndView authorize(@RequestParam Map<String, String> parameters,
            @PathVariable("realm") Optional<String> realmKey,
            Authentication authentication, HttpServletRequest request) {

        // TODO move everything to catch block and *always* parse request via factory

        if (!(authentication instanceof UserAuthentication) || !authentication.isAuthenticated()) {
            // check if prompt=none to return an error instead of triggering login
//            if (PromptMode.NONE.getValue().equals(promptParam)) {
//                // return error
//            }

            throw new InsufficientAuthenticationException("Invalid user authentication");
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;
        UserDetails userDetails = userAuth.getUser();

        String realm = SystemKeys.REALM_COMMON;
        if (realmKey.isPresent()) {
            realm = realmKey.get();
        }

        // fetch client here, if invalid no reason to process request..
        String clientId = parameters.get("client_id");
        if (!StringUtils.hasText(clientId)) {
            throw new InvalidClientException("A valid client id is required");
        }

        OAuth2ClientDetails clientDetails = oauth2ClientDetailsService.loadClientByClientId(clientId);

        try {
            // validate realm accessibility for client
            if (!realm.equals(clientDetails.getRealm()) && !realm.equals(SystemKeys.REALM_COMMON)) {
                throw new UnauthorizedClientException("client is not authorized for realm " + realm);
            }

            // validate and convert user for realm
            if (!realm.equals(userDetails.getRealm()) && !realm.equals(SystemKeys.REALM_COMMON)) {
                // user realm does not match, and client didn't request access via common
                throw new AccessDeniedException("user does not belong to required realm");
            }

            User user = userService.getUser(userDetails, clientDetails.getRealm());

            try {
                // extract unencoded parameters
                UriComponents uc = UriComponentsBuilder.fromUri(URI.create(request.getRequestURI()))
                        .query(request.getQueryString()).build(true);
                String state = uc.getQueryParams().getFirst("state");
                String nonce = uc.getQueryParams().getFirst("nonce");

                if (StringUtils.hasText(state)) {
                    parameters.put("state", state);
                }
                if (StringUtils.hasText(nonce)) {
                    parameters.put("nonce", nonce);
                }

                // fetch authorizationRequest via factory
                AuthorizationRequest authorizationRequest = oauth2AuthorizationRequestFactory
                        .createAuthorizationRequest(
                                parameters,
                                clientDetails, user);

                // resolve redirect now
                String redirectUri = authorizationRequest.getRedirectUri();
                String resolvedRedirectUri = redirectResolver.resolveRedirect(redirectUri, clientDetails);

                // we don't support requests without redirect
                // TODO evaluate
                if (!StringUtils.hasText(resolvedRedirectUri)) {
                    throw new RedirectMismatchException("No valid redirectUri in request");
                }

                // always use resolved redirect for request
                authorizationRequest.setRedirectUri(resolvedRedirectUri);

                // validate scopes
                oauth2AuthorizationRequestValidator.validateScope(authorizationRequest, clientDetails);

                // validate request via validator
                oauth2AuthorizationRequestValidator.validate(authorizationRequest, clientDetails, user);

                // evaluate approvals and let handlers modify request
                authorizationRequest = userApprovalHandler.checkForPreApproval(authorizationRequest, authentication);

                // check for preauthorization/autoapproval
                Boolean isApproved = userApprovalHandler.isApproved(authorizationRequest, authentication);

                // if response always update, we let handlers approve or deny request which are
                // already approved, otherwise they will return null
                if (isApproved != null) {
                    authorizationRequest.setApproved(isApproved);
                }

                // check if client asked for consent prompt
                boolean promptConsent = !authorizationRequest.isApproved();
                if (authorizationRequest.getExtensions().containsKey("prompt")) {
                    Set<String> prompt = StringUtils
                            .commaDelimitedListToSet((String) authorizationRequest.getExtensions().get("prompt"));
                    promptConsent = prompt.contains(PromptMode.CONSENT.getValue());
                }

                // check for offline_access, clients should always ask consent
                // we'll enforce it to complain with spec
                Set<String> scopes = authorizationRequest.getScope();
                if (scopes.contains(Config.SCOPE_OFFLINE_ACCESS)) {
                    // let firstParty skip, others should comply
                    if (!clientDetails.isFirstParty()) {
                        promptConsent = true;
                    }
                }

                // store request in repository for later use
                // note: this repo should be bound to session, or be able to distinguish between
                // them in a transparent way for consumers
                String key = oauth2AuthorizationRequestRepository.store(authorizationRequest);

                // check if request is fully approved or send to approval page
                if (authorizationRequest.isApproved() && !promptConsent) {

                    // check if response is requested via post
                    String responseMode = (String) authorizationRequest.getExtensions().get("response_mode");
                    if (ResponseMode.FORM_POST.getValue().equals(responseMode)) {
                        // process as authorized via form post
                        return new ModelAndView(formView + "?key=" + key);
                    }

                    // process as authorized via redirect
                    return new ModelAndView(responseView + "?key=" + key);
                }

                // send to user approval
                return new ModelAndView(userApprovalView + "?key=" + key);
            } catch (OAuth2Exception e) {
                logger.error("OAuth2 error " + e.getMessage());
                throw e;
            } catch (RuntimeException e) {
                logger.error("Exception " + e.getMessage());
                throw new ServerErrorException("Error", e);
            }
        } catch (OAuth2Exception e) {
            // try to build a error response
            String state = parameters.get("state");
            String responseMode = parameters.get("response_mode");
            String redirectUri = parameters.get("redirect_uri");
            String resolvedRedirectUri = null;
            try {
                resolvedRedirectUri = redirectResolver.resolveRedirect(redirectUri, clientDetails);
            } catch (Exception ee) {
                // ignore, we'll show an error page
            }

            // we don't support requests without redirect
            if (!StringUtils.hasText(resolvedRedirectUri)) {
                // will show generic error page
                throw new IllegalArgumentException("Requested redirect_uri " + String.valueOf(redirectUri)
                        + " is not a valid redirect");
            }

            if (ResponseMode.FORM_POST.getValue().equals(responseMode)) {
                // send form post with error
                Map<String, Object> model = buildErrorPost(resolvedRedirectUri, state, e);
                return new ModelAndView(formPage, model);
            }

            // send redirect with error
            String errorRedirect = buildErrorRedirect(resolvedRedirectUri, state, e,
                    isFragmentResponse(parameters));
            logger.trace("send error redirect to " + errorRedirect);

            // use a modelview, should be rewritten..
            ModelAndView redirectView = new ModelAndView("redirect:" + errorRedirect);
            // as per security BP use 303 to ensure POST are rewritten as GET
            redirectView.setStatus(HttpStatus.SEE_OTHER);
            return redirectView;
        } catch (RuntimeException e) {
            // send to error page
            Map<String, Serializable> model = new HashMap<>();
            model.put("error", e);

            return new ModelAndView(errorView, model);
        }
    }

    @RequestMapping(value = AUTHORIZED_URL, method = {
            RequestMethod.GET, RequestMethod.POST })
    public View authorized(@RequestParam String key,
            Authentication authentication) {

        if (!(authentication instanceof UserAuthentication) || !authentication.isAuthenticated()) {
            throw new InsufficientAuthenticationException("Invalid user authentication");
        }

        if (!StringUtils.hasText(key)) {
//            throw new IllegalArgumentException("Missing or invalid request key");

            // send to errorPage
            RedirectView redirectView = new RedirectView(ErrorEndpoint.ERROR_URL);
            // as per security BP use 303 to ensure POST are rewritten as GET
            redirectView.setStatusCode(HttpStatus.SEE_OTHER);
            return redirectView;
        }

        // ensure single use
        try {
            AuthorizationRequest authorizationRequest = oauth2AuthorizationRequestRepository.find(key);
            if (authorizationRequest == null) {
                throw new IllegalArgumentException("Missing or invalid request");
            }

            boolean asFragment = isFragmentResponse(authorizationRequest);

            // evaluate if approved
            if (!authorizationRequest.isApproved()) {
                // send redirect with error
                String errorRedirect = buildErrorRedirect(authorizationRequest,
                        new UserDeniedAuthorizationException("Access denied"), asFragment);
                RedirectView redirectView = new RedirectView(errorRedirect);
                // as per security BP use 303 to ensure POST are rewritten as GET
                redirectView.setStatusCode(HttpStatus.SEE_OTHER);
                return redirectView;
            }

            // load client
            String clientId = authorizationRequest.getClientId();
            if (!StringUtils.hasText(clientId)) {
                throw new InvalidClientException("A valid client id is required");
            }

            OAuth2ClientDetails clientDetails = oauth2ClientDetailsService.loadClientByClientId(clientId);

            // build response
            try {
                AuthorizationResponse authorizationResponse = buildResponse(authorizationRequest, clientDetails,
                        authentication);

                // send redirect with success
                String successRedirect = buildSuccessRedirect(authorizationRequest, authorizationResponse, asFragment);

                logger.trace("send success redirect to " + successRedirect);
                RedirectView redirectView = new RedirectView(successRedirect);
                // as per security BP use 303 to ensure POST are rewritten as GET
                redirectView.setStatusCode(HttpStatus.SEE_OTHER);
                return redirectView;
            } catch (OAuth2Exception e) {
                // bubble up state in params
                if (authorizationRequest.getState() != null) {
                    e.addAdditionalInformation("state", authorizationRequest.getState());
                }

                throw e;

            }
        } catch (OAuth2Exception e) {
            // try to build a error response
            AuthorizationRequest authorizationRequest = oauth2AuthorizationRequestRepository.find(key);
            if (authorizationRequest == null) {
                throw new IllegalArgumentException("Missing or invalid request");
            }

            // send redirect with error
            String errorRedirect = buildErrorRedirect(authorizationRequest, e,
                    isFragmentResponse(authorizationRequest));

            logger.trace("send error redirect to " + errorRedirect);
            RedirectView redirectView = new RedirectView(errorRedirect);
            // as per security BP use 303 to ensure POST are rewritten as GET
            redirectView.setStatusCode(HttpStatus.SEE_OTHER);
            return redirectView;
        } finally {
            // remove from repo, response is final
            oauth2AuthorizationRequestRepository.remove(key);
        }
    }

    @RequestMapping(value = FORM_POST_URL, method = {
            RequestMethod.GET, RequestMethod.POST })
    public ModelAndView authorizedFormPost(@RequestParam String key,
            Authentication authentication) {

        if (!(authentication instanceof UserAuthentication) || !authentication.isAuthenticated()) {
            throw new InsufficientAuthenticationException("Invalid user authentication");
        }

        if (!StringUtils.hasText(key)) {
            // send to errorPage
            Map<String, Serializable> model = new HashMap<>();
            model.put("error", "invalid_key");
            return new ModelAndView(errorView, model);
        }

        // ensure single use
        try {
            AuthorizationRequest authorizationRequest = oauth2AuthorizationRequestRepository.find(key);
            if (authorizationRequest == null) {
                throw new IllegalArgumentException("Missing or invalid request");
            }

            // check if response is requested via post
            String responseMode = (String) authorizationRequest.getExtensions().get("response_mode");
            if (!ResponseMode.FORM_POST.getValue().equals(responseMode)) {
                // send to errorPage
                Map<String, Serializable> model = new HashMap<>();
                model.put("error", "invalid_request");
                return new ModelAndView(errorView, model);
            }

            // evaluate if approved
            if (!authorizationRequest.isApproved()) {
                // send form with error
                Map<String, Object> model = buildErrorPost(authorizationRequest,
                        new UserDeniedAuthorizationException("Access denied"));
                return new ModelAndView(formPage, model);
            }

            // load client
            String clientId = authorizationRequest.getClientId();
            if (!StringUtils.hasText(clientId)) {
                throw new InvalidClientException("A valid client id is required");
            }

            OAuth2ClientDetails clientDetails = oauth2ClientDetailsService.loadClientByClientId(clientId);

            // build response
            try {
                AuthorizationResponse authorizationResponse = buildResponse(authorizationRequest, clientDetails,
                        authentication);

                // send form with success
                Map<String, Object> model = buildSuccessPost(authorizationRequest, authorizationResponse);

                logger.trace("send success post to " + String.valueOf(authorizationRequest.getRedirectUri()));
                return new ModelAndView(formPage, model);
            } catch (OAuth2Exception e) {
                // bubble up state in params
                if (authorizationRequest.getState() != null) {
                    e.addAdditionalInformation("state", authorizationRequest.getState());
                }

                throw e;

            }
        } catch (OAuth2Exception e) {
            // try to build a error response
            AuthorizationRequest authorizationRequest = oauth2AuthorizationRequestRepository.find(key);
            if (authorizationRequest == null) {
                throw new IllegalArgumentException("Missing or invalid request");
            }

            // send form with error
            Map<String, Object> model = buildErrorPost(authorizationRequest, e);

            logger.trace("send error post to " + String.valueOf(authorizationRequest.getRedirectUri()));
            return new ModelAndView(formPage, model);
        } finally {
            // remove from repo, response is final
            oauth2AuthorizationRequestRepository.remove(key);
        }
    }

    /*
     * Request handling
     */

    private AuthorizationResponse buildResponse(AuthorizationRequest authorizationRequest,
            OAuth2ClientDetails clientDetails, Authentication authentication) {
        String code = null;
        OAuth2AccessToken accessToken = null;
        IdToken idToken = null;

        // fetch all response types
        Set<String> responseTypes = authorizationRequest.getResponseTypes();

        if (responseTypes.contains("code")) {
            // convert request to a serializable model
            OAuth2Request oauth2Request = oauth2AuthorizationRequestFactory
                    .createOAuth2Request(authorizationRequest, clientDetails);
            OAuth2Authentication oauth2Authentication = new OAuth2Authentication(oauth2Request, authentication);

            // generate code and store binded to request
            code = authorizationCodeServices.createAuthorizationCode(oauth2Authentication);
            if (code == null) {
                throw new ServerErrorException("Unable to build response");
            }

        }

        // workaround for adding custom claims to id_token
        // TODO rework with proper integration of claimService with idTokenService and
        // split from accessToken claims with dedicated mapping
        if (responseTypes.contains("token")
                || (responseTypes.contains("id_token") && clientDetails.isIdTokenClaims())) {
            // translate to tokenRequest
            TokenRequest tokenRequest = oauth2TokenRequestFactory.createTokenRequest(authorizationRequest,
                    "implicit");

            // validate
            oauth2TokenRequestValidator.validate(tokenRequest, clientDetails);

            OAuth2Request oauth2Request = oauth2AuthorizationRequestFactory
                    .createOAuth2Request(authorizationRequest, clientDetails);

            // build request
            // TODO update granters
            org.springframework.security.oauth2.provider.implicit.ImplicitTokenRequest implicitTokenRequet = new org.springframework.security.oauth2.provider.implicit.ImplicitTokenRequest(
                    tokenRequest,
                    oauth2Request);

            // get token from granter, if flow is unsupported result will be null
            accessToken = tokenGranter.grant(implicitTokenRequet.getGrantType(), implicitTokenRequet);
            if (accessToken == null) {
                throw new ServerErrorException("Unable to build response");
            }

        }

        if (responseTypes.contains("id_token")) {
            // validate scope openid
            if (!authorizationRequest.getScope().contains("openid")) {
                throw new InvalidRequestException("openid scope is required to obtain an id_token");
            }
            // convert request to a serializable model
            OAuth2Request oauth2Request = oauth2AuthorizationRequestFactory
                    .createOAuth2Request(authorizationRequest, clientDetails);
            OAuth2Authentication oauth2Authentication = new OAuth2Authentication(oauth2Request, authentication);

            if (code != null && accessToken != null) {
                idToken = idTokenServices.createIdToken(oauth2Authentication, accessToken, code);
            } else if (code != null && accessToken == null) {
                idToken = idTokenServices.createIdToken(oauth2Authentication, code);
            } else if (accessToken != null && code == null) {
                idToken = idTokenServices.createIdToken(oauth2Authentication, accessToken);
            } else {
                idToken = idTokenServices.createIdToken(oauth2Authentication);
            }

            if (idToken == null) {
                throw new ServerErrorException("Unable to build response");
            }

        }

        // workaround for adding custom claims to id_token
        // TODO rework with proper integration of claimService with idTokenService and
        // split from accessToken claims with dedicated mapping
        if (responseTypes.size() == 1 && responseTypes.contains("id_token")) {
            // reset accessToken to return a proper response
            accessToken = null;
        }

        AuthorizationResponse response = new AuthorizationResponse();
        if (accessToken != null) {
            response = new AuthorizationResponse(accessToken);
        }

        if (code != null) {
            response.setCode(code);
        }

        if (idToken != null) {
            response.setIdToken(idToken.getValue());
        }

        // set issuer
        response.setIssuer(issuer);

        // append state params as per spec if provided
        // note: we append state even when empty
        if (authorizationRequest.getState() != null) {
            response.setState(authorizationRequest.getState());
        }

        return response;
    }

    private String responseTypeResult(String responseType) {
        if ("code".equals(responseType)) {
            return "code";
        } else if ("token".equals(responseType)) {
            return "access_token";
        } else if ("id_token".equals(responseType)) {
            return "id_token";
        } else {
            throw new InvalidRequestException("unsupported or invalid response type");
        }
    }

    private boolean isFragmentResponse(Map<String, String> parameters) {
        // check if responseMode in request
        String responseMode = parameters.get("response_mode");
        if (StringUtils.hasText(responseMode)) {
            return "fragment".equals(responseMode);
        }

        // check response types
        List<String> responseTypes = Arrays
                .asList(StringUtils.delimitedListToStringArray(parameters.get("response_type"), " "));
        if (responseTypes.contains("token") || responseTypes.contains("id_token")) {
            return true;
        }

        return false;
    }

    private boolean isFragmentResponse(AuthorizationRequest authorizationRequest) {
        // check if responseMode in request
        String responseMode = (String) authorizationRequest.getExtensions().get("response_mode");
        if (StringUtils.hasText(responseMode)) {
            return "fragment".equals(responseMode);
        }

        // check response types
        Set<String> responseTypes = authorizationRequest.getResponseTypes();
        if (responseTypes.contains("token") || responseTypes.contains("id_token")) {
            return true;
        }

        return false;
    }

    private String buildSuccessRedirect(AuthorizationRequest authorizationRequest,
            AuthorizationResponse authorizationResponse,
            boolean asFragment) {

        if (authorizationRequest == null || authorizationRequest.getRedirectUri() == null) {
            // no redirect, we'll show an exception to final user
            throw new UnapprovedClientAuthenticationException("Authorization request error.");
        }

        String redirectUri = authorizationRequest.getRedirectUri();

        // extract response as params for uri
        Map<String, Serializable> params = new HashMap<>();
        if (authorizationResponse.getCode() != null) {
            params.put("code", authorizationResponse.getCode());
        }
        if (authorizationResponse.getState() != null) {
            params.put("state", authorizationResponse.getState());
        }
        if (authorizationResponse.getAccessToken() != null) {
            params.put("access_token", authorizationResponse.getAccessToken());
        }
        if (authorizationResponse.getIdToken() != null) {
            params.put("id_token", authorizationResponse.getIdToken());
        }
        if (authorizationResponse.getTokenType() != null) {
            params.put("token_type", authorizationResponse.getTokenType());
        }
        if (authorizationResponse.getExpiresIn() != null) {
            params.put("expires_in", authorizationResponse.getExpiresIn());
        }
        if (authorizationResponse.getScope() != null) {
            String scope = StringUtils.collectionToDelimitedString(authorizationResponse.getScope(), " ");
            params.put("scope", scope);
        }
        if (authorizationResponse.getIssuer() != null) {
            params.put("iss", authorizationResponse.getIssuer());
        }

        return buildUri(redirectUri, params, asFragment);

    }

    private String buildErrorRedirect(AuthorizationRequest authorizationRequest, OAuth2Exception ex,
            boolean asFragment) {
        if (authorizationRequest == null || authorizationRequest.getRedirectUri() == null) {
            // no redirect, we'll show an exception to final user
            throw new UnapprovedClientAuthenticationException("Authorization request error.", ex);
        }

        return buildErrorRedirect(authorizationRequest.getRedirectUri(), authorizationRequest.getState(),
                ex, asFragment);
    }

    private String buildErrorRedirect(String redirectUri, String state, OAuth2Exception ex,
            boolean asFragment) {

        // translate error, mimic oauth2Exception serializer
        Map<String, Serializable> params = new HashMap<>();
        params.put("error", ex.getOAuth2ErrorCode());
        params.put("error_description", ex.getMessage());

        if (ex.getAdditionalInformation() != null) {
            for (Map.Entry<String, String> info : ex.getAdditionalInformation().entrySet()) {
                params.put(info.getKey(), info.getValue());
            }
        }

        // append state params as per spec if provided
        // note: we append state even when empty
        if (state != null) {
            params.put("state", state);
        }

        return buildUri(redirectUri, params, asFragment);

    }

    private String buildUri(String base, Map<String, Serializable> params, boolean asFragment) {

        // use uriBuilder
        UriComponentsBuilder template = UriComponentsBuilder.newInstance();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(base);
        URI redirectUri;
        try {
            // build with uri already encoded
            redirectUri = builder.build(true).toUri();
        } catch (Exception e) {
            // explicitely urlencode here
            redirectUri = builder.build().toUri();
            builder = UriComponentsBuilder.fromUri(redirectUri);
        }

        // update template with params from base
        template
                .scheme(redirectUri.getScheme())
                .host(redirectUri.getHost())
                .port(redirectUri.getPort())
                .userInfo(redirectUri.getUserInfo())
                .path(redirectUri.getPath());

        if (asFragment) {
            // append params as fragment
            List<String> values = new ArrayList<>();

            if (StringUtils.hasText(redirectUri.getFragment())) {
                values.add(redirectUri.getFragment());
            }

            for (String key : params.keySet()) {
                if (!"state".equals(key)) {
                    values.add(key + "={" + key + "}");
                }
            }

            template.fragment(StringUtils.collectionToDelimitedString(values, "&"));

            // expand and encode as fragment
            UriComponents encoded = template.build().expand(params).encode();
            String fragment = encoded.getFragment();
            // set state as pre-encoded
            if (params.containsKey("state")) {
                fragment = fragment.concat("&state=" + params.get("state"));
            }
            builder.fragment(fragment);

        } else {
            // append as query
            if (StringUtils.hasText(redirectUri.getQuery())) {
                template.query(redirectUri.getQuery());
            }

            for (String key : params.keySet()) {
                template.queryParam(key, "{" + key + "}");
            }

            // expand and encode as query
            UriComponents encoded = template.build().expand(params).encode();
            builder.query(encoded.getQuery());
            // replace state as pre-encoded
            if (params.containsKey("state")) {
                builder.replaceQueryParam("state", params.get("state"));
            }
        }

        // build
        return builder.build(true).toUriString();

    }

    private Map<String, Object> buildSuccessPost(AuthorizationRequest authorizationRequest,
            AuthorizationResponse authorizationResponse) {

        if (authorizationRequest == null || authorizationRequest.getRedirectUri() == null) {
            // no redirect, we'll show an exception to final user
            throw new UnapprovedClientAuthenticationException("Authorization request error.");
        }

        String redirectUri = authorizationRequest.getRedirectUri();

        // extract response as params for post
        Map<String, Serializable> params = new HashMap<>();
        if (authorizationResponse.getCode() != null) {
            params.put("code", authorizationResponse.getCode());
        }
        if (authorizationResponse.getState() != null) {
            params.put("state", authorizationResponse.getState());
        }
        if (authorizationResponse.getAccessToken() != null) {
            params.put("access_token", authorizationResponse.getAccessToken());
        }
        if (authorizationResponse.getIdToken() != null) {
            params.put("id_token", authorizationResponse.getIdToken());
        }
        if (authorizationResponse.getTokenType() != null) {
            params.put("token_type", authorizationResponse.getTokenType());
        }
        if (authorizationResponse.getExpiresIn() != null) {
            params.put("expires_in", Integer.toString(authorizationResponse.getExpiresIn()));
        }
        if (authorizationResponse.getScope() != null) {
            String scope = StringUtils.collectionToDelimitedString(authorizationResponse.getScope(), " ");
            params.put("scope", scope);
        }
        if (authorizationResponse.getIssuer() != null) {
            params.put("iss", authorizationResponse.getIssuer());
        }
        return buildPost(redirectUri, params);

    }

    private Map<String, Object> buildErrorPost(AuthorizationRequest authorizationRequest, OAuth2Exception ex) {
        if (authorizationRequest == null || authorizationRequest.getRedirectUri() == null) {
            // no redirect, we'll show an exception to final user
            throw new UnapprovedClientAuthenticationException("Authorization request error.", ex);
        }

        return buildErrorPost(authorizationRequest.getRedirectUri(), authorizationRequest.getState(),
                ex);
    }

    private Map<String, Object> buildErrorPost(String redirectUri, String state, OAuth2Exception ex) {

        // translate error, mimic oauth2Exception serializer
        Map<String, Serializable> params = new HashMap<>();
        params.put("error", ex.getOAuth2ErrorCode());
        params.put("error_description", ex.getMessage());

        if (ex.getAdditionalInformation() != null) {
            for (Map.Entry<String, String> info : ex.getAdditionalInformation().entrySet()) {
                params.put(info.getKey(), info.getValue());
            }
        }

        // append state params as per spec if provided
        // note: we append state even when empty
        if (state != null) {
            params.put("state", state);
        }

        return buildPost(redirectUri, params);

    }

    private Map<String, Object> buildPost(String redirectUri, Map<String, Serializable> params) {

        Map<String, Object> model = new HashMap<>();
        model.put("redirectUri", redirectUri);
        model.putAll(params);

        return model;

    }

    /*
     * Exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ModelAndView handleRuntimeException(RuntimeException e) throws Exception {
        logger.error("Error: " + e.getMessage());
        if (logger.isTraceEnabled()) {
            e.printStackTrace();
        }

        // send to error page
        Map<String, Serializable> model = new HashMap<>();
        model.put("error", e);

        return new ModelAndView(errorView, model);
    }

    @ExceptionHandler(OAuth2Exception.class)
    public ModelAndView handleOAuth2Exception(OAuth2Exception e) throws Exception {
        logger.error("Error: " + e.getMessage());
        if (logger.isTraceEnabled()) {
            e.printStackTrace();
        }

        // send to error page
        Map<String, Serializable> model = new HashMap<>();
        model.put("error", e);

        return new ModelAndView(errorView, model);
    }

}
