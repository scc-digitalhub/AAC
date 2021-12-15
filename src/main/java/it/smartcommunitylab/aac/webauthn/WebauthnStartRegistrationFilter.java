package it.smartcommunitylab.aac.webauthn;

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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.webauthn.auth.RegisterCredentialAuthenticationToken;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAuthenticationException;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserAccountService;

//auth/webauthn/startRegistration/acbd
public class WebauthnStartRegistrationFilter extends AbstractAuthenticationProcessingFilter {
    public static final String DEFAULT_FILTER_URI = WebAuthnLoginAuthenticationEntryPoint.DEFAULT_FILTER_URI;
    public static final String DEFAULT_LOGIN_URI = WebAuthnLoginAuthenticationEntryPoint.DEFAULT_LOGIN_URI;

    private final RequestMatcher requestMatcher;
    private AuthenticationEntryPoint authenticationEntryPoint;
    private final WebAuthnUserAccountService userAccountService;

    private final ProviderRepository<WebAuthnIdentityProviderConfig> registrationRepository;

    public WebauthnStartRegistrationFilter(WebAuthnUserAccountService userAccountService,
            ProviderRepository<WebAuthnIdentityProviderConfig> registrationRepository) {
        this(userAccountService, registrationRepository, DEFAULT_FILTER_URI, null);
    }

    public WebauthnStartRegistrationFilter(WebAuthnUserAccountService userAccountService,
            ProviderRepository<WebAuthnIdentityProviderConfig> registrationRepository,
            String filterProcessingUrl,
            AuthenticationEntryPoint authenticationEntryPoint) {
        super(filterProcessingUrl);

        this.userAccountService = userAccountService;
        Assert.hasText(filterProcessingUrl, "filterProcessesUrl must contain a URL pattern");
        Assert.isTrue(
                filterProcessingUrl
                        .contains("{" + WebAuthnLoginAuthenticationEntryPoint.PROVIDER_URI_VARIABLE_NAME + "}"),
                "filterProcessesUrl must contain a {" + WebAuthnLoginAuthenticationEntryPoint.PROVIDER_URI_VARIABLE_NAME
                        + "} match variable");
        this.registrationRepository = registrationRepository;

        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);
        setRequiresAuthenticationRequestMatcher(requestMatcher);

        // redirect failed attempts to webauthn login
        this.authenticationEntryPoint = new WebAuthnLoginAuthenticationEntryPoint(
                WebAuthnLoginAuthenticationEntryPoint.SUPER_LOGIN_URI, DEFAULT_LOGIN_URI,
                filterProcessingUrl);
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
                // // pass error message as attribute - does not work with redirect..
                // // TODO either switch to controller or use session
                // // alternatively fall back to an error page instead of calling entrypoint
                // request.setAttribute("authException", exception);

                // from SimpleUrlAuthenticationFailureHandler, save exception as session
                HttpSession session = request.getSession(true);
                if (session != null) {
                    request.getSession().setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
                }

                authenticationEntryPoint.commence(request, response, exception);
            }
        });
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {

        if (!requestMatcher.matches(request)) {
            return null;
        }

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new HttpRequestMethodNotSupportedException(request.getMethod(), new String[] { "POST" });
        }

        String providerId = requestMatcher.matcher(request).getVariables().get("registrationId");
        WebAuthnIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(providerId);

        if (providerConfig == null) {
            throw new ProviderNotFoundException("no provider or realm found for this request");
        }

        String realm = providerConfig.getRealm();
        request.setAttribute("realm", realm);

        String username = request.getParameter("username");

        if (!StringUtils.hasText(username)) {
            AuthenticationException e = new BadCredentialsException("invalid username");
            throw new WebAuthnAuthenticationException(e.getMessage());
        }

        // fetch account to check
        // if this does not exists we'll let authProvider handle the error to ensure
        // proper audit
        WebAuthnUserAccount account = userAccountService.findByUsername(realm, username);

        if (account == null) {
            AuthenticationException e = new BadCredentialsException("invalid username");
            throw new WebAuthnAuthenticationException(e.getMessage());
        }

        // build a request
        RegisterCredentialAuthenticationToken authenticationRequest = new RegisterCredentialAuthenticationToken(
                username);

        ProviderWrappedAuthenticationToken wrappedAuthRequest = new ProviderWrappedAuthenticationToken(
                authenticationRequest,
                providerId, SystemKeys.AUTHORITY_WEBAUTHN);

        // also collect request details
        WebAuthenticationDetails webAuthenticationDetails = new WebAuthenticationDetails(request);

        // set details
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
}
