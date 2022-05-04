package it.smartcommunitylab.aac.openid.apple.auth;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.RealmAwareAuthenticationEntryPoint;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.openid.apple.AppleIdentityAuthority;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfig;

/*
 * Custom oauth2 login filter, handles login via auth code 
 * interacts with extended auth manager to process login requests per realm
 */
public class AppleLoginAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String DEFAULT_FILTER_URI = AppleIdentityAuthority.AUTHORITY_URL + "login/{registrationId}";

    private final RequestMatcher requestMatcher;

    // we need to load client registration
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository;
    private AuthenticationEntryPoint authenticationEntryPoint;

    // we use this to persist request before redirect, and here to fetch details
    private AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;

    public AppleLoginAuthenticationFilter(
            ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository,
            ClientRegistrationRepository clientRegistrationRepository) {
        this(registrationRepository, clientRegistrationRepository, DEFAULT_FILTER_URI, null);
    }

    public AppleLoginAuthenticationFilter(
            ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository,
            ClientRegistrationRepository clientRegistrationRepository,
            String filterProcessingUrl, AuthenticationEntryPoint authenticationEntryPoint) {
        super(filterProcessingUrl);
        Assert.notNull(registrationRepository, "provider registration repository cannot be null");
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
        Assert.hasText(filterProcessingUrl, "filterProcessesUrl must contain a URL pattern");
        Assert.isTrue(filterProcessingUrl.contains("{registrationId}"),
                "filterProcessesUrl must contain a {registrationId} match variable");

        this.registrationRepository = registrationRepository;
        this.clientRegistrationRepository = clientRegistrationRepository;

        // we need to build a custom requestMatcher to extract variables from url
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);
        setRequiresAuthenticationRequestMatcher(requestMatcher);

        // redirect failed attempts to login
        this.authenticationEntryPoint = new RealmAwareAuthenticationEntryPoint("/login");
        if (authenticationEntryPoint != null) {
            this.authenticationEntryPoint = authenticationEntryPoint;
        }

        // enforce session id change to prevent fixation attacks
        setAllowSessionCreation(true);
        setSessionAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy());

        // use a custom failureHandler to return to login form
        setAuthenticationFailureHandler(new AuthenticationFailureHandler() {
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                    AuthenticationException exception) throws IOException, ServletException {

                // from SimpleUrlAuthenticationFailureHandler, save exception as session
                HttpSession session = request.getSession(true);
                if (session != null) {
                    request.getSession().setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
                }

                getAuthenticationEntryPoint().commence(request, response, exception);
            }
        });

    }

    public final void setAuthorizationRequestRepository(
            AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository) {
        Assert.notNull(authorizationRequestRepository, "authorizationRequestRepository cannot be null");
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        if (!requestMatcher.matches(request)) {
            return null;
        }

        // fetch registrationId
        String providerId = requestMatcher.matcher(request).getVariables().get("registrationId");

        // load config, we don't want to handle invalid requests at all
        AppleIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(providerId);
        if (providerConfig == null) {
            OAuth2Error oauth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }

        String realm = providerConfig.getRealm();
        // set as attribute to enable fallback to login on error
        request.setAttribute("realm", realm);

        // fetch params
        String code = request.getParameter(OAuth2ParameterNames.CODE);
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        String error = request.getParameter(OAuth2ParameterNames.ERROR);
        String errorDescription = request.getParameter(OAuth2ParameterNames.ERROR_DESCRIPTION);
        String errorUri = request.getParameter(OAuth2ParameterNames.ERROR_URI);

        // check if request is valid
        if (!isAuthorizationResponse(state, code, error)) {
            OAuth2Error oauth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }

        Map<String, String> params = new HashMap<>();
        params.put(OAuth2ParameterNames.STATE, state);
        params.put(OAuth2ParameterNames.CODE, code);
        params.put(OAuth2ParameterNames.ERROR, error);
        params.put(OAuth2ParameterNames.ERROR_DESCRIPTION, errorDescription);
        params.put(OAuth2ParameterNames.ERROR_URI, errorUri);

        // consume request via state
        OAuth2AuthorizationRequest authorizationRequest = authorizationRequestRepository
                .removeAuthorizationRequest(request, response);

        if (authorizationRequest == null) {
            OAuth2Error oauth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }

        // fetch client
        String registrationId = authorizationRequest.getAttribute(OAuth2ParameterNames.REGISTRATION_ID);
        ClientRegistration clientRegistration = this.clientRegistrationRepository.findByRegistrationId(registrationId);
        if (clientRegistration == null) {
            OAuth2Error oauth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }

        if (!registrationId.equals(providerId)) {
            // response doesn't belong here...
            OAuth2Error oauth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }

        String redirectUri = UriComponentsBuilder.fromHttpUrl(UrlUtils.buildFullRequestUrl(request))
                .replaceQuery(null)
                .build()
                .toUriString();

        // rebuild request
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.from(authorizationRequest);

        // add openid scope to request to satisfy provider
        // TODO remove hack and write a dedicated provider
        Set<String> scopes = new HashSet<>();
        scopes.addAll(clientRegistration.getScopes());
        scopes.add("openid");
        builder.scopes(scopes);

        // check if we received a `user` profile along with code and store it
        // we keep this in request because response can't store it...
        String user = request.getParameter("user");
        if (user != null) {
            // store as is to let provider decode it
            Map<String, Object> additionalParams = new HashMap<>();
            additionalParams.putAll(authorizationRequest.getAdditionalParameters());
            additionalParams.put("user", user);
            builder.additionalParameters(additionalParams);
        }

        authorizationRequest = builder.build();

        // convert request to response
        OAuth2AuthorizationResponse authorizationResponse = convert(params, redirectUri);

        // collect info for webauth as additional details
        Object authenticationDetails = this.authenticationDetailsSource.buildDetails(request);

        // build login token with params
        OAuth2LoginAuthenticationToken authenticationRequest = new OAuth2LoginAuthenticationToken(clientRegistration,
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse));
        authenticationRequest.setDetails(authenticationDetails);

        // wrap auth request for multi-provider manager
        // providerId is registrationId
        ProviderWrappedAuthenticationToken wrappedAuthRequest = new ProviderWrappedAuthenticationToken(
                authenticationRequest,
                providerId, SystemKeys.AUTHORITY_APPLE);

        // also collect request details
        WebAuthenticationDetails webAuthenticationDetails = new WebAuthenticationDetails(request);
        wrappedAuthRequest.setAuthenticationDetails(webAuthenticationDetails);

        // authenticate via extended authManager
        UserAuthentication userAuthentication = (UserAuthentication) getAuthenticationManager()
                .authenticate(wrappedAuthRequest);

        // return authentication to be set in security context
        return userAuthentication;
    }

    private boolean isAuthorizationResponse(String state, String code, String error) {
        return (StringUtils.hasText(state) && StringUtils.hasText(code))
                || (StringUtils.hasText(state) && StringUtils.hasText(error));

    }

    private static OAuth2AuthorizationResponse convert(Map<String, String> params, String redirectUri) {
        String code = params.get(OAuth2ParameterNames.CODE);
        String errorCode = params.get(OAuth2ParameterNames.ERROR);
        String state = params.get(OAuth2ParameterNames.STATE);

        if (StringUtils.hasText(code)) {
            return OAuth2AuthorizationResponse.success(code)
                    .redirectUri(redirectUri)
                    .state(state)
                    .build();
        } else {
            String errorDescription = params.get(OAuth2ParameterNames.ERROR_DESCRIPTION);
            String errorUri = params.get(OAuth2ParameterNames.ERROR_URI);
            return OAuth2AuthorizationResponse.error(errorCode)
                    .redirectUri(redirectUri)
                    .errorDescription(errorDescription)
                    .errorUri(errorUri)
                    .state(state)
                    .build();
        }
    }

    protected AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return authenticationEntryPoint;
    }

    public void setAuthenticationEntryPoint(AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

}
