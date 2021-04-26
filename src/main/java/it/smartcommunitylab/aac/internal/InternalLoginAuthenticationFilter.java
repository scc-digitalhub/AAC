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
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;

/*
 * Handles login requests for internal authority, via extended auth manager
 */
public class InternalLoginAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String DEFAULT_FILTER_URI = "/auth/internal/";
    public static final String ACTION = "login";
    public static final String REALM_URI_VARIABLE_NAME = "realm";
    public static final String PROVIDER_URI_VARIABLE_NAME = "provider";

    private final RequestMatcher realmRequestMatcher;
    private final RequestMatcher providerRequestMatcher;
    private final RequestMatcher providerRealmRequestMatcher;

    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final InternalUserAccountService userAccountService;

    public InternalLoginAuthenticationFilter(InternalUserAccountService userAccountService) {
        this(userAccountService, DEFAULT_FILTER_URI, new RealmAwareAuthenticationEntryPoint(
                "/" + ACTION));
    }

    public InternalLoginAuthenticationFilter(InternalUserAccountService userAccountService,
            String filterProcessingUrl,
            AuthenticationEntryPoint authenticationEntryPoint) {
        super(filterProcessingUrl + ACTION);
        Assert.notNull(userAccountService, "user account service is required");
        this.userAccountService = userAccountService;

        // build a matcher for all requests
        RequestMatcher baseRequestMatcher = new AntPathRequestMatcher(filterProcessingUrl + ACTION);

        realmRequestMatcher = new AntPathRequestMatcher(
                "/-/{" + REALM_URI_VARIABLE_NAME + "}" + filterProcessingUrl + ACTION);

        providerRequestMatcher = new AntPathRequestMatcher(
                filterProcessingUrl + ACTION + "/{" + PROVIDER_URI_VARIABLE_NAME + "}");

        providerRealmRequestMatcher = new AntPathRequestMatcher(
                "/-/{" + REALM_URI_VARIABLE_NAME + "}" + filterProcessingUrl + ACTION + "/{"
                        + PROVIDER_URI_VARIABLE_NAME + "}");

        // use both the global and the realm matcher
        RequestMatcher requestMatcher = new OrRequestMatcher(
                baseRequestMatcher,
                providerRequestMatcher,
                realmRequestMatcher,
                providerRealmRequestMatcher);
        setRequiresAuthenticationRequestMatcher(requestMatcher);

        // redirect failed attempts to login
        this.authenticationEntryPoint = authenticationEntryPoint;

        // TODO evaluate adopting changeSessionStrategy to avoid fixation attacks
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

        // we support only POST requests
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new HttpRequestMethodNotSupportedException(request.getMethod(), new String[] { "POST" });
        }

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new NotRegisteredException();
        }

        // discover realm and/or provider
        String realm = null;
        String providerId = null;

        // check realm+provider
        if (providerRealmRequestMatcher.matches(request)) {
            // resolve variables
            realm = providerRealmRequestMatcher.matcher(request).getVariables()
                    .get(REALM_URI_VARIABLE_NAME);
            // resolve realm variable
            providerId = providerRealmRequestMatcher.matcher(request).getVariables()
                    .get(PROVIDER_URI_VARIABLE_NAME);
        }

        // check realm
        if (realmRequestMatcher.matches(request)) {
            // resolve realm variable
            realm = realmRequestMatcher.matcher(request).getVariables()
                    .get(REALM_URI_VARIABLE_NAME);
        }

        // check provider
        if (providerRequestMatcher.matches(request)) {
            // resolve provider variable
            providerId = providerRequestMatcher.matcher(request).getVariables()
                    .get(PROVIDER_URI_VARIABLE_NAME);
        }

        if (!StringUtils.hasText(providerId)) {
            // TODO handle COMMON realm via discovery or additinal params
            throw new ProviderNotFoundException("no provider or realm found for this request");
//            // wrap as realm
//            if (!StringUtils.hasText(realm)) {
//                // global realm
//                realm = SystemKeys.REALM_GLOBAL;
//            }
//
//            wrappedAuthRequest = new RealmWrappedAuthenticationToken(authenticationRequest,
//                    realm, SystemKeys.AUTHORITY_INTERNAL);
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

        WrappedAuthenticationToken wrappedAuthRequest = new ProviderWrappedAuthenticationToken(authenticationRequest,
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

    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return authenticationEntryPoint;
    }

}
