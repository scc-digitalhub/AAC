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

package it.smartcommunitylab.aac.eidas.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.RealmAwareAuthenticationEntryPoint;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.eidas.EIDASIdentityAuthority;
import it.smartcommunitylab.aac.eidas.provider.EIDASIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.auth.Saml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.saml.auth.SerializableSaml2AuthenticationRequestContext;
import it.smartcommunitylab.aac.saml.service.HttpSessionSaml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.saml.service.Saml2AuthenticationTokenConverter;
import it.smartcommunitylab.aac.spid.events.SpidAuthenticationResponseEvent;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Collects the SAML responses coming back from the eIDAS node and turns them into an authentication
 * request for the eidas authority.
 *
 * This is the one piece that cannot be inherited from SPID: SpidWebSsoAuthenticationFilter routes
 * the response to the spid authority (hardcoded in the wrapped token) and keeps its collaborators
 * private, so the flow is reproduced here bound to the eidas authority.
 */
public class EIDASWebSsoAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private static final Logger logger = LoggerFactory.getLogger(EIDASWebSsoAuthenticationFilter.class);

    public static final String DEFAULT_FILTER_URI = EIDASIdentityAuthority.AUTHORITY_URL + "sso/{registrationId}";
    private static final char PATH_DELIMITER = '/';
    private static final String authorityId = SystemKeys.AUTHORITY_EIDAS;

    private final RequestMatcher requestMatcher;
    private final ProviderConfigRepository<EIDASIdentityProviderConfig> providerConfigRepository;
    private final Saml2AuthenticationTokenConverter authenticationConverter;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    // default session attribute: it must match the one Saml2AuthenticationTokenConverter reads
    private Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> authenticationRequestRepository =
        new HttpSessionSaml2AuthenticationRequestRepository();

    public EIDASWebSsoAuthenticationFilter(
        ProviderConfigRepository<EIDASIdentityProviderConfig> providerConfigRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository
    ) {
        this(providerConfigRepository, relyingPartyRegistrationRepository, DEFAULT_FILTER_URI, null);
    }

    public EIDASWebSsoAuthenticationFilter(
        ProviderConfigRepository<EIDASIdentityProviderConfig> providerConfigRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
        String filterProcessingUrl,
        AuthenticationEntryPoint authenticationEntryPoint
    ) {
        super(filterProcessingUrl);
        Assert.notNull(providerConfigRepository, "provider registration repository cannot be null");
        Assert.notNull(relyingPartyRegistrationRepository, "relyingPartyRegistrationRepository cannot be null");
        Assert.hasText(filterProcessingUrl, "filterProcessesUrl must contain a URL pattern");

        this.providerConfigRepository = providerConfigRepository;
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);

        this.authenticationConverter =
            new Saml2AuthenticationTokenConverter(
                buildRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository)
            );
        setRequiresAuthenticationRequestMatcher(requestMatcher);

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
                    // the SPID handler is silent: the exception goes to the session and the user is sent back
                    // to login, nothing reaches the log. Say what failed, at debug, with the saml2 error code
                    // when there is one: it is the difference between a diagnosis and a guess
                    if (logger.isDebugEnabled()) {
                        String code = exception instanceof Saml2AuthenticationException
                            ? ((Saml2AuthenticationException) exception).getSaml2Error().getErrorCode()
                            : exception.getClass().getSimpleName();
                        logger.debug(
                            "eidas authentication failed for {}: [{}] {}",
                            request.getRequestURI(),
                            code,
                            exception.getMessage(),
                            exception
                        );
                    }

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
     * the relying party registration is recovered from the authentication request that started this
     * flow, and its template fields (eg. {baseUrl}) resolved against the incoming request
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
            if (relyingPartyRegistration == null) {
                return null;
            }

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

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException, IOException, ServletException {
        // A. extract provider - registrationId is base64url({providerId})
        String registrationId = resolveRegistrationId(request);
        String providerId = SpidIdentityProviderConfig.decodeRegistrationId(registrationId);
        EIDASIdentityProviderConfig providerConfig = providerConfigRepository.findByProviderId(providerId);
        if (providerConfig == null) {
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                "No relying party registration found"
            );
            throw new Saml2AuthenticationException(saml2Error);
        }
        // set realm as request attribute to enable fallback to login on error
        request.setAttribute("realm", providerConfig.getRealm());

        // B. generate token from this response
        Saml2AuthenticationToken authenticationRequest = authenticationConverter.convert(request);
        if (authenticationRequest == null) {
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                "No relying party registration found"
            );
            throw new Saml2AuthenticationException(saml2Error);
        }

        // C. we handle only responses to locally initiated sessions
        SerializableSaml2AuthenticationRequestContext authnReqContext =
            authenticationRequestRepository.loadAuthenticationRequest(request);
        if (authnReqContext == null) {
            // no request in session: the browser did not send the session cookie with the (cross-site) POST, or
            // the RelayState does not match what was saved. Reported as such, the generic code is misleading
            logger.debug(
                "eidas response for {} has no matching authentication request in session (session {}, relayState {})",
                request.getRequestURI(),
                request.getSession(false) != null ? "present" : "absent",
                request.getParameter("RelayState")
            );
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.INVALID_DESTINATION,
                "Wrong destination for response: no matching authentication request in session"
            );
            throw new Saml2AuthenticationException(saml2Error);
        }
        String authRequestRegistrationId = authnReqContext.getAssertingPartyRegistrationId();
        RelyingPartyRegistration registration = authenticationRequest.getRelyingPartyRegistration();
        if (!registration.getRegistrationId().equals(authRequestRegistrationId)) {
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.INVALID_DESTINATION,
                "Wrong destination for response"
            );
            throw new Saml2AuthenticationException(saml2Error);
        }

        if (eventPublisher != null) {
            eventPublisher.publishEvent(
                new SpidAuthenticationResponseEvent(
                    authorityId,
                    providerConfig.getProvider(),
                    providerConfig.getRealm(),
                    authenticationRequest
                )
            );
        }

        // D. hand it to the multi-provider auth manager, on the eidas authority
        ProviderWrappedAuthenticationToken wrappedAuthRequest = new ProviderWrappedAuthenticationToken(
            authenticationRequest,
            providerId,
            SystemKeys.AUTHORITY_EIDAS
        );

        // also collect request details
        WebAuthenticationDetails webAuthenticationDetails = new WebAuthenticationDetails(request);
        wrappedAuthRequest.setAuthenticationDetails(webAuthenticationDetails);
        return (UserAuthentication) getAuthenticationManager().authenticate(wrappedAuthRequest);
    }

    /*
     * the registration id the response belongs to, read from the path (/sso/{registrationId}): the one
     * point where the provider is identified, kept overridable for endpoints without the path variable
     */
    protected String resolveRegistrationId(HttpServletRequest request) {
        return requestMatcher.matcher(request).getVariables().get("registrationId");
    }

    protected Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> getAuthenticationRequestRepository() {
        return authenticationRequestRepository;
    }

    public void setAuthenticationRequestRepository(
        Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> authenticationRequestRepository
    ) {
        this.authenticationRequestRepository = authenticationRequestRepository;
    }
}
