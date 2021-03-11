package it.smartcommunitylab.aac.openid.service;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.openid.OIDCAuthority;

/*
 * Custom oauth2 login filter, handles login via auth code 
 * interacts with extended auth manager to process login requests per realm
 */
public class OIDCLoginAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String DEFAULT_FILTER_URI = OIDCAuthority.AUTHORITY_URL + "login/*";

    // we need to load client registration
    private final ClientRegistrationRepository clientRegistrationRepository;

    // we use this to persist request before redirect, and here to fetch details
    private AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;

    public OIDCLoginAuthenticationFilter(ClientRegistrationRepository clientRegistrationRepository) {
        this(clientRegistrationRepository, DEFAULT_FILTER_URI);

    }

    public OIDCLoginAuthenticationFilter(ClientRegistrationRepository clientRegistrationRepository,
            String filterProcessesUrl) {
        super(filterProcessesUrl);
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
        this.clientRegistrationRepository = clientRegistrationRepository;

    }

    public final void setAuthorizationRequestRepository(
            AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository) {
        Assert.notNull(authorizationRequestRepository, "authorizationRequestRepository cannot be null");
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

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

        String redirectUri = UriComponentsBuilder.fromHttpUrl(UrlUtils.buildFullRequestUrl(request))
                .replaceQuery(null)
                .build()
                .toUriString();

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
        String providerId = registrationId;
        ProviderWrappedAuthenticationToken wrappedAuthRequest = new ProviderWrappedAuthenticationToken(
                SystemKeys.AUTHORITY_OIDC, providerId, authenticationRequest);

        //also collect request details
        WebAuthenticationDetails webAuthenticationDetails = new WebAuthenticationDetails(request);
        wrappedAuthRequest.setAuthenticationDetails(webAuthenticationDetails);
        
        // authenticate via extended authManager
        UserAuthenticationToken userAuthentication = (UserAuthenticationToken) getAuthenticationManager()
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

}
