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

package it.smartcommunitylab.aac.saml.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.RealmAwareAuthenticationEntryPoint;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.saml.SamlIdentityAuthority;
import it.smartcommunitylab.aac.saml.service.Saml2AuthenticationTokenConverter;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.service.HttpSessionSaml2AuthenticationRequestRepository;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class SamlWebSsoAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String DEFAULT_FILTER_URI = SamlIdentityAuthority.AUTHORITY_URL + "sso/{registrationId}";

    private final RequestMatcher requestMatcher;

    private final ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository;
    private final Saml2AuthenticationTokenConverter authenticationConverter;

    private Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> authenticationRequestRepository =
        new HttpSessionSaml2AuthenticationRequestRepository();

    private AuthenticationEntryPoint authenticationEntryPoint;

    public SamlWebSsoAuthenticationFilter(
        ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository
    ) {
        this(registrationRepository, relyingPartyRegistrationRepository, DEFAULT_FILTER_URI, null);
    }

    public SamlWebSsoAuthenticationFilter(
        ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
        String filterProcessingUrl,
        AuthenticationEntryPoint authenticationEntryPoint
    ) {
        super(filterProcessingUrl);
        Assert.notNull(registrationRepository, "provider registration repository cannot be null");
        Assert.notNull(relyingPartyRegistrationRepository, "relyingPartyRegistrationRepository cannot be null");
        Assert.hasText(filterProcessingUrl, "filterProcessesUrl must contain a URL pattern");
        Assert.isTrue(
            filterProcessingUrl.contains("{registrationId}"),
            "filterProcessesUrl must contain a {registrationId} match variable"
        );

        this.registrationRepository = registrationRepository;

        // we need to build a custom requestMatcher to extract variables from url
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);
        setRequiresAuthenticationRequestMatcher(requestMatcher);

        // build a default resolver, will lookup by registrationId
        DefaultRelyingPartyRegistrationResolver registrationResolver = new DefaultRelyingPartyRegistrationResolver(
            relyingPartyRegistrationRepository
        );
        // use the default token converter
        authenticationConverter =
            new Saml2AuthenticationTokenConverter((RelyingPartyRegistrationResolver) registrationResolver);

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

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        return (
            super.requiresAuthentication(request, response) && StringUtils.hasText(request.getParameter("SAMLResponse"))
        );
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException {
        if (!requestMatcher.matches(request)) {
            return null;
        }

        // fetch registrationId
        String providerId = requestMatcher.matcher(request).getVariables().get("registrationId");

        // registrationId is providerId
        SamlIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(providerId);
        if (providerConfig == null) {
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                "No relying party registration found"
            );
            throw new Saml2AuthenticationException(saml2Error);
        }

        String realm = providerConfig.getRealm();
        // set as attribute to enable fallback to login on error
        request.setAttribute("realm", realm);

        // use converter to fetch rpRegistration and parse saml response
        Saml2AuthenticationToken authenticationRequest = authenticationConverter.convert(request);
        if (authenticationRequest == null) {
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                "No relying party registration found"
            );
            throw new Saml2AuthenticationException(saml2Error);
        }

        // fetch request, we handle only responses to locally initiated sessions
        SerializableSaml2AuthenticationRequestContext authenticationContext =
            authenticationRequestRepository.loadAuthenticationRequest(request);
        if (authenticationContext == null) {
            // response doesn't belong here...
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.INVALID_DESTINATION,
                "Wrong destination for response"
            );
            throw new Saml2AuthenticationException(saml2Error);
        }
        String registrationId = authenticationContext.getRelyingPartyRegistrationId();

        // fetch rp registration
        RelyingPartyRegistration registration = authenticationRequest.getRelyingPartyRegistration();

        if (!registrationId.equals(providerId) || !registration.getRegistrationId().equals(registrationId)) {
            // response doesn't belong here...
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.INVALID_DESTINATION,
                "Wrong destination for response"
            );
            throw new Saml2AuthenticationException(saml2Error);
        }

        // TODO add extended validation for inresponseTo, authClassesRef, NameIdFormat
        // etc..

        // collect info for webauth as additional details
        //        Object authenticationDetails = this.authenticationDetailsSource.buildDetails(request);

        // wrap auth request for multi-provider manager
        // providerId is registrationId
        ProviderWrappedAuthenticationToken wrappedAuthRequest = new ProviderWrappedAuthenticationToken(
            authenticationRequest,
            providerId,
            SystemKeys.AUTHORITY_SAML
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

    protected AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return authenticationEntryPoint;
    }

    public void setAuthenticationEntryPoint(AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    public void setAuthenticationRequestRepository(
        Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> authenticationRequestRepository
    ) {
        this.authenticationRequestRepository = authenticationRequestRepository;
    }
}
