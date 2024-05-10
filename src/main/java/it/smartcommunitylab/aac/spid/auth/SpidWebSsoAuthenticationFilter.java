/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.RealmAwareAuthenticationEntryPoint;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.saml.auth.Saml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.saml.auth.SerializableSaml2AuthenticationRequestContext;
import it.smartcommunitylab.aac.saml.service.HttpSessionSaml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.saml.service.Saml2AuthenticationTokenConverter;
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
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
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/*
 * SpidWebSsoAuthenticationRequestFilter is the filter that intercepts SPID SAML authentication "responses" and
 * generates a (Spring) Authentication with embedded authentication token.
 * This is a filter accordingly with the Spring Security architecture.
 * For more on how SPID authentication responses are made, see the
 *  https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html
 * For more on the Spring Security architecture and why an AuthenticationFilter is required, see
 *  https://docs.spring.io/spring-security/reference/servlet/saml2/login/overview.html
 */
public class SpidWebSsoAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String DEFAULT_FILTER_URI = SpidIdentityAuthority.AUTHORITY_URL + "sso/{registrationId}";
    private static final char PATH_DELIMITER = '/';

    private final RequestMatcher requestMatcher;
    private final ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRegistrationRepository;
    private final Saml2AuthenticationTokenConverter authenticationConverter;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> authenticationRequestRepository =
        new HttpSessionSaml2AuthenticationRequestRepository(
            HttpSessionSaml2AuthenticationRequestRepository.class.getName() + ".SPID_AUTHORIZATION_REQUEST"
        );

    public SpidWebSsoAuthenticationFilter(
        ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository
    ) {
        this(registrationRepository, relyingPartyRegistrationRepository, DEFAULT_FILTER_URI, null);
    }

    public SpidWebSsoAuthenticationFilter(
        ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRegistrationRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
        String filterProcessingUrl,
        AuthenticationEntryPoint authenticationEntryPoint
    ) {
        super(filterProcessingUrl);
        Assert.notNull(providerConfigRegistrationRepository, "provider registration repository cannot be null");
        Assert.notNull(relyingPartyRegistrationRepository, "relyingPartyRegistrationRepository cannot be null");
        Assert.hasText(filterProcessingUrl, "filterProcessesUrl must contain a URL pattern");

        this.providerConfigRegistrationRepository = providerConfigRegistrationRepository;
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);

        // build a default resolver, will lookup by registrationId, to create default token converter
        //        DefaultRelyingPartyRegistrationResolver registrationResolver = new DefaultRelyingPartyRegistrationResolver(
        //            relyingPartyRegistrationRepository
        //        );
        //        this.authenticationConverter =
        //            new Saml2AuthenticationTokenConverter((HttpServletRequest request, String relyingPartyRegistrationId) -> {
        //                SerializableSaml2AuthenticationRequestContext ctx =
        //                    authenticationRequestRepository.loadAuthenticationRequest(request);
        //                if (ctx == null) {
        //                    return null;
        //                }
        //                String regId = ctx.getAssertingPartyRegistrationId();
        //                return relyingPartyRegistrationRepository.findByRegistrationId(regId);
        //            });
        this.authenticationConverter =
            new Saml2AuthenticationTokenConverter(
                buildRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository)
            );
        setRequiresAuthenticationRequestMatcher(requestMatcher); // required by superclass logic and definition in order to intercept the /sso endpoint

        // redirect failed attempts to login
        this.authenticationEntryPoint =
            Objects.requireNonNullElseGet(
                authenticationEntryPoint,
                () -> new RealmAwareAuthenticationEntryPoint("/login")
            );

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

    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return authenticationEntryPoint;
    }

    /*
     * buildRelyingPartyRegistrationResolver build a resolver that recovers a relying party registration
     * for an authentication response. The registration is actually recovered from the authentication
     * request, which can be accesses from the request context.
     */
    private RelyingPartyRegistrationResolver buildRelyingPartyRegistrationResolver(
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository
    ) {
        return (HttpServletRequest request, String relyingPartyRegistrationId) -> {
            SerializableSaml2AuthenticationRequestContext ctx =
                authenticationRequestRepository.loadAuthenticationRequest(request);
            if (ctx == null) {
                return null;
            }
            String regId = ctx.getAssertingPartyRegistrationId();
            RelyingPartyRegistration relyingPartyRegistration = relyingPartyRegistrationRepository.findByRegistrationId(
                regId
            );
            // resolve template fields of the registration, such as {baseUrl}
            String applicationUri = getApplicationUri(request);
            Function<String, String> templateResolver = templateResolver(applicationUri, relyingPartyRegistration);
            String relyingPartyEntityId = templateResolver.apply(relyingPartyRegistration.getEntityId());
            String assertionConsumerServiceLocation = templateResolver.apply(
                relyingPartyRegistration.getAssertionConsumerServiceLocation()
            );
            String singleLogoutServiceLocation = templateResolver.apply(
                relyingPartyRegistration.getSingleLogoutServiceLocation()
            );
            String singleLogoutServiceResponseLocation = templateResolver.apply(
                relyingPartyRegistration.getSingleLogoutServiceResponseLocation()
            );
            return RelyingPartyRegistration
                .withRelyingPartyRegistration(relyingPartyRegistration)
                .entityId(relyingPartyEntityId)
                .assertionConsumerServiceLocation(assertionConsumerServiceLocation)
                .singleLogoutServiceLocation(singleLogoutServiceLocation)
                .singleLogoutServiceResponseLocation(singleLogoutServiceResponseLocation)
                .build();
        };
    }

    private static String getApplicationUri(HttpServletRequest request) {
        UriComponents uriComponents = UriComponentsBuilder
            .fromHttpUrl(UrlUtils.buildFullRequestUrl(request))
            .replacePath(request.getContextPath())
            .replaceQuery(null)
            .fragment(null)
            .build();
        return uriComponents.toUriString();
    }

    private Function<String, String> templateResolver(String applicationUri, RelyingPartyRegistration relyingParty) {
        return template -> resolveUrlTemplate(template, applicationUri, relyingParty);
    }

    private static String resolveUrlTemplate(String template, String baseUrl, RelyingPartyRegistration relyingParty) {
        if (template == null) {
            return null;
        }
        String entityId = relyingParty.getAssertingPartyDetails().getEntityId();
        String registrationId = relyingParty.getRegistrationId();
        Map<String, String> uriVariables = new HashMap<>();
        UriComponents uriComponents = UriComponentsBuilder
            .fromHttpUrl(baseUrl)
            .replaceQuery(null)
            .fragment(null)
            .build();
        String scheme = uriComponents.getScheme();
        uriVariables.put("baseScheme", (scheme != null) ? scheme : "");
        String host = uriComponents.getHost();
        uriVariables.put("baseHost", (host != null) ? host : "");
        // following logic is based on HierarchicalUriComponents#toUriString()
        int port = uriComponents.getPort();
        uriVariables.put("basePort", (port == -1) ? "" : ":" + port);
        String path = uriComponents.getPath();
        if (StringUtils.hasLength(path) && path.charAt(0) != PATH_DELIMITER) {
            path = PATH_DELIMITER + path;
        }
        uriVariables.put("basePath", (path != null) ? path : "");
        uriVariables.put("baseUrl", uriComponents.toUriString());
        uriVariables.put("entityId", StringUtils.hasText(entityId) ? entityId : "");
        uriVariables.put("registrationId", StringUtils.hasText(registrationId) ? registrationId : "");
        return UriComponentsBuilder.fromUriString(template).buildAndExpand(uriVariables).toUriString();
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        return (
            super.requiresAuthentication(request, response) && StringUtils.hasText(request.getParameter("SAMLResponse"))
        );
    }

    /*
     * attemptAuthentication is invoked by the doFilter of the parent class.
     * This class usage behaviour can be described as follows:
     * (1) if returns valid Authentication -> passed to successfulAuthentication
     * (2) if invalid, throws AuthenticationException -> passed to unsuccessfulAuthentication
     * (3) if returns null -> incomplete authentication, assumes that it is completed later on
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException, IOException, ServletException {
        // A. extract provider
        String registrationId = requestMatcher.matcher(request).getVariables().get("registrationId");
        // registrationId is base64UrlEncode({registrationId})
        String providerId = SpidIdentityProviderConfig.decodeRegistrationId(registrationId);
        SpidIdentityProviderConfig providerConfig = providerConfigRegistrationRepository.findByProviderId(providerId);
        if (providerConfig == null) {
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                "No relying party registration found"
            );
            throw new Saml2AuthenticationException(saml2Error);
        }
        // set realm as request request attribute to enable fallback to login on error
        String realm = providerConfig.getRealm();
        request.setAttribute("realm", realm);

        // B. generate token from this response
        // use converter to fetch rpRegistration and parse saml response
        Saml2AuthenticationToken authenticationRequest = authenticationConverter.convert(request);
        if (authenticationRequest == null) {
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                "No relying party registration found"
            );
            throw new Saml2AuthenticationException(saml2Error);
        }

        // C. extract request associated to this response for extra validation - we handle only responses to locally initiated sessions
        // check that initiating request exists
        SerializableSaml2AuthenticationRequestContext authnReqContext =
            authenticationRequestRepository.loadAuthenticationRequest(request);
        if (authnReqContext == null) {
            // response doesn't belong here...
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.INVALID_DESTINATION,
                "Wrong destination for response"
            );
            throw new Saml2AuthenticationException(saml2Error);
        }
        // chat that auth request and initiating request has matching RP registration id
        String authRequestRegistrationId = authnReqContext.getAssertingPartyRegistrationId();
        RelyingPartyRegistration registration = authenticationRequest.getRelyingPartyRegistration();
        if (!registration.getRegistrationId().equals(authRequestRegistrationId)) {
            // response doesn't belong here...
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.INVALID_DESTINATION,
                "Wrong destination for response"
            );
            throw new Saml2AuthenticationException(saml2Error);
        }

        // D. generate a wrapped authentication for the multi-provider auth manager
        ProviderWrappedAuthenticationToken wrappedAuthRequest = new ProviderWrappedAuthenticationToken(
            authenticationRequest,
            providerId,
            SystemKeys.AUTHORITY_SPID
        );

        // also collect request details
        WebAuthenticationDetails webAuthenticationDetails = new WebAuthenticationDetails(request);
        wrappedAuthRequest.setAuthenticationDetails(webAuthenticationDetails);
        return (UserAuthentication) getAuthenticationManager().authenticate(wrappedAuthRequest);
    }

    public void setAuthenticationRequestRepository(
        Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> authenticationRequestRepository
    ) {
        this.authenticationRequestRepository = authenticationRequestRepository;
    }
}
