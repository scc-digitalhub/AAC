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
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
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
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Objects;

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

    private final RequestMatcher requestMatcher;
    private final ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository;
    private final Saml2AuthenticationTokenConverter authenticationConverter;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> authenticationRequestRepository =
        new HttpSessionSaml2AuthenticationRequestRepository();

    public SpidWebSsoAuthenticationFilter(
        ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository
    ) {
        this(registrationRepository, relyingPartyRegistrationRepository, DEFAULT_FILTER_URI, null);
    }

    public SpidWebSsoAuthenticationFilter(
        ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
        String filterProcessingUrl,
        AuthenticationEntryPoint authenticationEntryPoint
    ) {
        super(filterProcessingUrl);
        Assert.notNull(registrationRepository, "provider registration repository cannot be null");
        Assert.notNull(relyingPartyRegistrationRepository, "relyingPartyRegistrationRepository cannot be null");
        Assert.hasText(filterProcessingUrl, "filterProcessesUrl must contain a URL pattern");

        this.registrationRepository = registrationRepository;
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);

        // build a default resolver, will lookup by registrationId, to create default token converter
        DefaultRelyingPartyRegistrationResolver registrationResolver = new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository);
        this.authenticationConverter = new Saml2AuthenticationTokenConverter((RelyingPartyRegistrationResolver) registrationResolver);
        setRequiresAuthenticationRequestMatcher(requestMatcher); // required by superclass logic and definition in order to intercept the /sso endpoint

        // redirect failed attempts to login
        this.authenticationEntryPoint = Objects.requireNonNullElseGet(
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

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        return (super.requiresAuthentication(request, response)
                && StringUtils.hasText(request.getParameter("SAMLResponse")));
    }

    /*
     * attemptAuthentication is invoked by the doFilter of the parent class
     * this class usage behaviour is as follows:
     * (1) if returns valid Authentication -> passed to successfulAuthentication
     * (2) if invalid, throws AuthenticationException -> passed to unsuccessfulAuthentication
     * (3) if returns null -> incomplete authentication, assumes that it is completed later on
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        // A. extract provider
        String registrationId = requestMatcher.matcher(request).getVariables().get("registrationId");
        String providerId = SpidIdentityProviderConfig.getProviderId(registrationId); // registrationId is providerId+IdPkey
        SpidIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(providerId);
        if (providerConfig == null) {
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                "No relying party registration found");
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
        SerializableSaml2AuthenticationRequestContext authnReqContext = authenticationRequestRepository.loadAuthenticationRequest(request);
        if (authnReqContext == null) {
            // response doesn't belong here...
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.INVALID_DESTINATION,
                "Wrong destination for response"
            );
            throw new Saml2AuthenticationException(saml2Error);
        }
        // chat that auth request and initiating request has matching RP registration id
        String authReqRPRegistrationId = authnReqContext.getRelyingPartyRegistrationId();
        RelyingPartyRegistration registration = authenticationRequest.getRelyingPartyRegistration();
        if (!registration.getRegistrationId().equals(authReqRPRegistrationId)) {
            // response doesn't belong here...
            Saml2Error saml2Error = new Saml2Error(Saml2ErrorCodes.INVALID_DESTINATION,
                    "Wrong destination for response");
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

    public void setAuthenticationRequestRepository(Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> authenticationRequestRepository) {
        this.authenticationRequestRepository = authenticationRequestRepository;
    }
}
