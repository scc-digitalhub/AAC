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

package it.smartcommunitylab.aac.password.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.internal.auth.InternalAuthenticationException;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.password.PasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.password.service.InternalPasswordJpaUserCredentialsService;
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

/*
 * Handles login requests for internal authority, via extended auth manager
 */
public class UsernamePasswordAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String DEFAULT_FILTER_URI = PasswordIdentityAuthority.AUTHORITY_URL + "login/{registrationId}";
    public static final String DEFAULT_LOGIN_URI = PasswordIdentityAuthority.AUTHORITY_URL + "form/{registrationId}";

    private final RequestMatcher requestMatcher;

    private final ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository;

    private AuthenticationEntryPoint authenticationEntryPoint;

    // TODO remove
    private final UserAccountService<InternalUserAccount> userAccountService;

    // TODO remove
    private final InternalPasswordJpaUserCredentialsService userPasswordService;

    public UsernamePasswordAuthenticationFilter(
        UserAccountService<InternalUserAccount> userAccountService,
        InternalPasswordJpaUserCredentialsService userPasswordService,
        ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository
    ) {
        this(userAccountService, userPasswordService, registrationRepository, DEFAULT_FILTER_URI, null);
    }

    public UsernamePasswordAuthenticationFilter(
        UserAccountService<InternalUserAccount> userAccountService,
        InternalPasswordJpaUserCredentialsService userPasswordService,
        ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository,
        String filterProcessingUrl,
        AuthenticationEntryPoint authenticationEntryPoint
    ) {
        super(filterProcessingUrl);
        Assert.notNull(userAccountService, "user account service is required");
        Assert.notNull(userPasswordService, "password service is required");

        Assert.notNull(registrationRepository, "provider registration repository cannot be null");
        Assert.hasText(filterProcessingUrl, "filterProcessesUrl must contain a URL pattern");
        Assert.isTrue(
            filterProcessingUrl.contains("{registrationId}"),
            "filterProcessesUrl must contain a {registrationId} match variable"
        );

        this.userAccountService = userAccountService;
        this.userPasswordService = userPasswordService;
        this.registrationRepository = registrationRepository;

        // we need to build a custom requestMatcher to extract variables from url
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);
        setRequiresAuthenticationRequestMatcher(requestMatcher);

        // redirect failed attempts to login
        //        this.authenticationEntryPoint = new RealmAwareAuthenticationEntryPoint("/login");

        // redirect failed attempts to internal login
        this.authenticationEntryPoint =
            new InternalPasswordLoginAuthenticationEntryPoint("/login", DEFAULT_LOGIN_URI, filterProcessingUrl);
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
        PasswordIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(providerId);

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
            AuthenticationException e = new BadCredentialsException("invalid user or password");
            throw new InternalAuthenticationException(null, username, password, "password", e, e.getMessage());
        }

        // DISABLED, TODO implement filter
        //        // fetch account to check
        //        // if this does not exists we'll let authProvider handle the error to ensure
        //        // proper audit
        //        // TODO rework, this should be handled post login by adding another filter
        //        String repositoryId = providerConfig.getRepositoryId();
        //        InternalUserAccount account = userAccountService.findAccountById(repositoryId, username);
        //        // fetch active password
        //        InternalUserPassword credentials = userPasswordService.findPassword(repositoryId, username);
        //
        //        if (account != null && credentials != null) {
        //            HttpSession session = request.getSession(true);
        //            if (session != null) {
        //                // check if user needs to reset password, and add redirect
        //                if (credentials.isChangeOnFirstAccess()) {
        //                    // TODO build url
        //                    session.setAttribute(RequestAwareAuthenticationSuccessHandler.SAVED_REQUEST,
        //                            "/changepwd/" + providerId + "/" + account.getUuid());
        //                }
        //            }
        //        }

        // build a request
        UsernamePasswordAuthenticationToken authenticationRequest = new UsernamePasswordAuthenticationToken(
            username,
            password
        );

        ProviderWrappedAuthenticationToken wrappedAuthRequest = new ProviderWrappedAuthenticationToken(
            authenticationRequest,
            providerId,
            SystemKeys.AUTHORITY_PASSWORD
        );

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
