/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.openidfed.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.RealmAwareAuthenticationEntryPoint;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.oidc.events.OAuth2AuthorizationResponseEvent;
import it.smartcommunitylab.aac.openidfed.OpenIdFedIdentityAuthority;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import it.smartcommunitylab.aac.users.auth.UserAuthentication;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
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

/*
 * Custom openid fed login filter, handles login via auth code with automatic client registration
 */
public class OpenIdFedLoginAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String DEFAULT_FILTER_URI = OpenIdFedIdentityAuthority.AUTHORITY_URL + "login/{providerId}";
    public static final String DEFAULT_EXTERNAL_REQ_STATE = "externalRequestDefaultStateString";

    private final RequestMatcher requestMatcher;
    private final String authority;

    // we need to load client registration
    private final ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository;
    private AuthenticationEntryPoint authenticationEntryPoint;

    // we use this to persist request before redirect, and here to fetch details
    private AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;

    public OpenIdFedLoginAuthenticationFilter(
        ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository
    ) {
        this(SystemKeys.AUTHORITY_OPENIDFED, registrationRepository, DEFAULT_FILTER_URI, null);
    }

    public OpenIdFedLoginAuthenticationFilter(
        String authority,
        ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository,
        String filterProcessingUrl,
        AuthenticationEntryPoint authenticationEntryPoint
    ) {
        super(filterProcessingUrl);
        Assert.hasText(authority, "authority can not be null or empty");
        Assert.notNull(registrationRepository, "provider registration repository cannot be null");
        Assert.hasText(filterProcessingUrl, "filterProcessesUrl must contain a URL pattern");
        Assert.isTrue(
            filterProcessingUrl.contains("{providerId}"),
            "filterProcessesUrl must contain a {providerId} match variable"
        );

        this.authority = authority;
        this.registrationRepository = registrationRepository;

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
        setAuthenticationFailureHandler(
            new AuthenticationFailureHandler() {
                public void onAuthenticationFailure(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    AuthenticationException exception
                ) throws IOException, ServletException {
                    // from SimpleUrlAuthenticationFailureHandler, save exception as session
                    HttpSession session = request.getSession(true);
                    if (session != null) {
                        request.getSession().setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
                    }

                    getAuthenticationEntryPoint().commence(request, response, exception);
                }
            }
        );
    }

    public final void setAuthorizationRequestRepository(
        AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository
    ) {
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
        String providerId = requestMatcher.matcher(request).getVariables().get("providerId");

        // load config, we don't want to handle invalid requests at all
        OpenIdFedIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(providerId);
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
        OAuth2AuthorizationRequest authorizationRequest = authorizationRequestRepository.removeAuthorizationRequest(
            request,
            response
        );

        if (authorizationRequest == null) {
            OAuth2Error oauth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }

        //recover registrationId from request attributes
        String registrationId = authorizationRequest.getAttribute(OAuth2ParameterNames.REGISTRATION_ID);

        // fetch client
        ClientRegistration clientRegistration = providerConfig
            .getClientRegistrationRepository()
            .findByRegistrationId(registrationId);
        if (clientRegistration == null) {
            OAuth2Error oauth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }

        //replace clientId placeholder
        String clientId = authorizationRequest.getClientId();
        clientRegistration = ClientRegistration.withClientRegistration(clientRegistration).clientId(clientId).build();

        String redirectUri = UriComponentsBuilder.fromHttpUrl(UrlUtils.buildFullRequestUrl(request))
            .replaceQuery(null)
            .fragment(null)
            .build()
            .toUriString();

        // rebuild request
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.from(authorizationRequest);

        // make sure params match
        builder.redirectUri(redirectUri);
        builder.state(state);

        //rebuild
        authorizationRequest = builder.build();

        // convert request to response
        OAuth2AuthorizationResponse authorizationResponse = convert(params, redirectUri);

        //publish event
        if (eventPublisher != null) {
            logger.debug("publish event for authorization response {}", authorizationRequest.getState());

            eventPublisher.publishEvent(
                new OAuth2AuthorizationResponseEvent(authority, providerId, realm, authorizationResponse)
            );
        }

        //check if we received an error
        if (authorizationResponse.getError() != null) {
            throw new OAuth2AuthenticationException(authorizationResponse.getError());
        }

        // collect info for webauth as additional details
        Object authenticationDetails = this.authenticationDetailsSource.buildDetails(request);

        // build login token with params
        OAuth2LoginAuthenticationToken authenticationRequest = new OAuth2LoginAuthenticationToken(
            clientRegistration,
            new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse)
        );
        authenticationRequest.setDetails(authenticationDetails);

        // wrap auth request for multi-provider manager
        // providerId is registrationId
        ProviderWrappedAuthenticationToken wrappedAuthRequest = new ProviderWrappedAuthenticationToken(
            authenticationRequest,
            providerId,
            authority
        );

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
        return (
            (StringUtils.hasText(state) && StringUtils.hasText(code)) ||
            (StringUtils.hasText(state) && StringUtils.hasText(error))
        );
    }

    private static OAuth2AuthorizationResponse convert(Map<String, String> params, String redirectUri) {
        String code = params.get(OAuth2ParameterNames.CODE);
        String errorCode = params.get(OAuth2ParameterNames.ERROR);
        String state = params.get(OAuth2ParameterNames.STATE);

        if (StringUtils.hasText(code)) {
            return OAuth2AuthorizationResponse.success(code).redirectUri(redirectUri).state(state).build();
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
