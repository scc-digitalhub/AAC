package it.smartcommunitylab.aac.webauthn.auth;

import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.exception.AssertionFailedException;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnLoginRpService;
import it.smartcommunitylab.aac.webauthn.store.WebAuthnAssertionRequestStore;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;

public class WebAuthnAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String DEFAULT_FILTER_URI = WebAuthnIdentityAuthority.AUTHORITY_URL + "login/{registrationId}";
    public static final String DEFAULT_LOGIN_URI = WebAuthnIdentityAuthority.AUTHORITY_URL + "form/{registrationId}";

    private final RequestMatcher requestMatcher;

    private final ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository;

    private AuthenticationEntryPoint authenticationEntryPoint;

    private final WebAuthnLoginRpService rpService;
    private final WebAuthnAssertionRequestStore requestStore;

    public WebAuthnAuthenticationFilter(
        WebAuthnLoginRpService rpService,
        WebAuthnAssertionRequestStore requestStore,
        ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository
    ) {
        this(rpService, requestStore, registrationRepository, DEFAULT_FILTER_URI, null);
    }

    public WebAuthnAuthenticationFilter(
        WebAuthnLoginRpService rpService,
        WebAuthnAssertionRequestStore requestStore,
        ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository,
        String filterProcessingUrl,
        AuthenticationEntryPoint authenticationEntryPoint
    ) {
        super(filterProcessingUrl);
        Assert.notNull(rpService, "rp service is required");
        Assert.notNull(requestStore, "request store is required");

        Assert.notNull(registrationRepository, "provider registration repository cannot be null");
        Assert.hasText(filterProcessingUrl, "filterProcessesUrl must contain a URL pattern");
        Assert.isTrue(
            filterProcessingUrl.contains("{registrationId}"),
            "filterProcessesUrl must contain a {registrationId} match variable"
        );

        this.rpService = rpService;
        this.requestStore = requestStore;
        this.registrationRepository = registrationRepository;

        // we need to build a custom requestMatcher to extract variables from url
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);
        setRequiresAuthenticationRequestMatcher(requestMatcher);

        // redirect failed attempts to login
        //        this.authenticationEntryPoint = new RealmAwareAuthenticationEntryPoint("/login");

        // redirect failed attempts to internal login
        this.authenticationEntryPoint =
            new WebAuthnLoginAuthenticationEntryPoint("/login", DEFAULT_LOGIN_URI, filterProcessingUrl);
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
                    //                // pass error message as attribute - does not work with redirect..
                    //                // TODO either switch to controller or use session
                    //                // alternatively fall back to an error page instead of calling entrypoint
                    //                request.setAttribute("authException", exception);

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
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException, IOException, ServletException {
        if (!requestMatcher.matches(request)) {
            return null;
        }

        // we support only POST requests
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new HttpRequestMethodNotSupportedException(request.getMethod(), new String[] { "POST" });
        }

        // fetch registrationId
        String providerId = requestMatcher.matcher(request).getVariables().get("registrationId");
        WebAuthnIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(providerId);

        if (providerConfig == null) {
            throw new ProviderNotFoundException("no provider or realm found for this request");
        }

        String realm = providerConfig.getRealm();
        // set as attribute to enable fallback to login on error
        request.setAttribute("realm", realm);

        // get params
        String key = request.getParameter("key");
        String assertion = request.getParameter("assertion");

        try {
            if (!StringUtils.hasText(key) || !StringUtils.hasText(assertion)) {
                throw new BadCredentialsException("invalid authentication assertion");
            }

            // fetch request and remove from store
            AssertionRequest assertionRequest = requestStore.consume(key);
            if (assertionRequest == null) {
                throw new BadCredentialsException("invalid authentication assertion");
            }

            // parse assertion and fetch response via service
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
                PublicKeyCredential.parseAssertionResponseJson(assertion);

            AssertionResult assertionResult = rpService.finishLogin(providerId, assertionRequest, pkc);
            String userHandle = new String(assertionResult.getUserHandle().getBytes());

            // build a request
            WebAuthnAuthenticationToken authenticationRequest = new WebAuthnAuthenticationToken(
                userHandle,
                assertionRequest,
                assertion,
                assertionResult
            );

            ProviderWrappedAuthenticationToken wrappedAuthRequest = new ProviderWrappedAuthenticationToken(
                authenticationRequest,
                providerId,
                SystemKeys.AUTHORITY_WEBAUTHN
            );

            // also collect request details
            WebAuthenticationDetails webAuthenticationDetails = new WebAuthenticationDetails(request);

            // set details
            authenticationRequest.setDetails(webAuthenticationDetails);
            wrappedAuthRequest.setAuthenticationDetails(webAuthenticationDetails);

            // authenticate via extended authManager
            UserAuthentication userAuthentication = (UserAuthentication) getAuthenticationManager()
                .authenticate(wrappedAuthRequest);

            // return authentication to be set in security context
            return userAuthentication;
        } catch (AssertionFailedException | IOException ie) {
            logger.debug(ie.getMessage());
            AuthenticationException e = new BadCredentialsException("invalid authentication assertion");
            throw new WebAuthnAuthenticationException(null, null, assertion, e, e.getMessage());
        } catch (BadCredentialsException e) {
            throw new WebAuthnAuthenticationException(null, null, assertion, e, e.getMessage());
        } catch (NoSuchUserException | NoSuchProviderException e) {
            AuthenticationException ex = new BadCredentialsException("invalid authentication assertion");
            throw new WebAuthnAuthenticationException(null, null, assertion, ex, e.getMessage());
        }
    }

    protected AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return authenticationEntryPoint;
    }

    public void setAuthenticationEntryPoint(AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }
}
