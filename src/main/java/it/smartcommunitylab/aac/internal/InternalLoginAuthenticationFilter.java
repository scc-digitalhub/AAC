package it.smartcommunitylab.aac.internal;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.bouncycastle.asn1.ocsp.ResponderID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NotRegisteredException;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.RealmAwareAuthenticationEntryPoint;
import it.smartcommunitylab.aac.core.auth.RealmWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.RequestAwareAuthenticationSuccessHandler;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.core.auth.WrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.saml.SamlIdentityAuthority;

/*
 * Handles login requests for internal authority, via extended auth manager
 */
public class InternalLoginAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String DEFAULT_FILTER_URI = InternalIdentityAuthority.AUTHORITY_URL + "login/{registrationId}";

    private final RequestMatcher requestMatcher;

    private final ProviderRepository<InternalIdentityProviderConfig> registrationRepository;

    private AuthenticationEntryPoint authenticationEntryPoint;
    private final InternalUserAccountService userAccountService;

    public InternalLoginAuthenticationFilter(InternalUserAccountService userAccountService,
            ProviderRepository<InternalIdentityProviderConfig> registrationRepository) {
        this(userAccountService, registrationRepository, DEFAULT_FILTER_URI, null);
    }

    public InternalLoginAuthenticationFilter(InternalUserAccountService userAccountService,
            ProviderRepository<InternalIdentityProviderConfig> registrationRepository,
            String filterProcessingUrl, AuthenticationEntryPoint authenticationEntryPoint) {
        super(filterProcessingUrl);
        Assert.notNull(userAccountService, "user account service is required");
        Assert.notNull(registrationRepository, "provider registration repository cannot be null");
        Assert.hasText(filterProcessingUrl, "filterProcessesUrl must contain a URL pattern");
        Assert.isTrue(filterProcessingUrl.contains("{registrationId}"),
                "filterProcessesUrl must contain a {registrationId} match variable");

        this.userAccountService = userAccountService;
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
        setAuthenticationFailureHandler(new AuthenticationFailureHandler() {
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                    AuthenticationException exception) throws IOException, ServletException {
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
        });
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws AuthenticationException, IOException, ServletException {

        if (!requestMatcher.matches(request)) {
            return null;
        }

        // we support only POST requests
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new HttpRequestMethodNotSupportedException(request.getMethod(), new String[] { "POST" });
        }

        // fetch registrationId
        String providerId = requestMatcher.matcher(request).getVariables().get("registrationId");
        InternalIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(providerId);

        if (providerConfig == null) {
            throw new ProviderNotFoundException("no provider or realm found for this request");
        }

        String realm = providerConfig.getRealm();
        // set as attribute to enable fallback to login on error
        request.setAttribute("realm", realm);

        // get params
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new NotRegisteredException();
        }

        // fetch account
        InternalUserAccount account = userAccountService.findAccountByUsername(realm, username);
        if (account == null) {
            // don't leak user does not exists
            throw new BadCredentialsException("invalid user or password");
        }

        HttpSession session = request.getSession(true);
        if (session != null) {
            // check if user needs to reset password, and add redirect
            if (account.isChangeOnFirstAccess()) {
                // TODO build url
                session.setAttribute(RequestAwareAuthenticationSuccessHandler.SAVED_REQUEST, "/pwdchange");
            }
        }

        // build a request
        UsernamePasswordAuthenticationToken authenticationRequest = new UsernamePasswordAuthenticationToken(username,
                password);

        ProviderWrappedAuthenticationToken wrappedAuthRequest = new ProviderWrappedAuthenticationToken(
                authenticationRequest,
                providerId, SystemKeys.AUTHORITY_INTERNAL);

        // also collect request details
        WebAuthenticationDetails webAuthenticationDetails = new WebAuthenticationDetails(request);

        // set details
        wrappedAuthRequest.setAuthenticationDetails(webAuthenticationDetails);

        // authenticate via extended authManager
        UserAuthenticationToken userAuthentication = (UserAuthenticationToken) getAuthenticationManager()
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
