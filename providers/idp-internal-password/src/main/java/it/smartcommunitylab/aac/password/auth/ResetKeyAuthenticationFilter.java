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
import it.smartcommunitylab.aac.core.auth.RealmAwareAuthenticationEntryPoint;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.internal.auth.InternalAuthenticationException;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.password.PasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.model.InternalUserPassword;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.password.service.InternalPasswordJpaUserCredentialsService;
import it.smartcommunitylab.aac.users.auth.UserAuthentication;
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
public class ResetKeyAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String DEFAULT_FILTER_URI =
        PasswordIdentityAuthority.AUTHORITY_URL + "doreset/{registrationId}";

    private final ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository;

    //    public static final String DEFAULT_FILTER_URI = "/auth/internal/";
    //    public static final String ACTION = "doreset";
    //    public static final String REALM_URI_VARIABLE_NAME = "realm";
    //    public static final String PROVIDER_URI_VARIABLE_NAME = "provider";

    private final RequestMatcher requestMatcher;

    //    private final RequestMatcher realmRequestMatcher;
    //    private final RequestMatcher providerRequestMatcher;
    //    private final RequestMatcher providerRealmRequestMatcher;

    private AuthenticationEntryPoint authenticationEntryPoint;

    // TODO remove services and build resetPassword action url after auth success
    private final UserAccountService<InternalUserAccount> userAccountService;
    private final InternalPasswordJpaUserCredentialsService userPasswordService;

    public ResetKeyAuthenticationFilter(
        UserAccountService<InternalUserAccount> userAccountService,
        InternalPasswordJpaUserCredentialsService userPasswordService,
        ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository
    ) {
        this(userAccountService, userPasswordService, registrationRepository, DEFAULT_FILTER_URI, null);
    }

    public ResetKeyAuthenticationFilter(
        UserAccountService<InternalUserAccount> userAccountService,
        InternalPasswordJpaUserCredentialsService userPasswordService,
        ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository,
        String filterProcessingUrl,
        AuthenticationEntryPoint authenticationEntryPoint
    ) {
        super(filterProcessingUrl);
        Assert.notNull(userAccountService, "user account service is required");
        Assert.notNull(userPasswordService, "password service is mandatory");
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

        // we support only GET requests
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            throw new HttpRequestMethodNotSupportedException(request.getMethod(), new String[] { "GET" });
        }

        // fetch registrationId
        String providerId = requestMatcher.matcher(request).getVariables().get("registrationId");
        PasswordIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(providerId);

        if (providerConfig == null) {
            throw new ProviderNotFoundException("no provider or realm found for this request");
        }

        String realm = providerConfig.getRealm();
        String repositoryId = providerConfig.getRepositoryId();

        // set as attribute to enable fallback to login on error
        request.setAttribute("realm", realm);

        try {
            String username = request.getParameter("username");
            String code = request.getParameter("code");

            if (!StringUtils.hasText(username) || !StringUtils.hasText(code)) {
                throw new BadCredentialsException("missing or invalid username or confirm code");
            }

            // // fetch account
            // InternalUserPassword password = userPasswordService.findCredentialsByResetKey(repositoryId, code);
            // if (password == null) {
            //     // don't leak password does not exists
            //     throw new BadCredentialsException("invalid-key");
            // }

            // String userId = password.getUserId();

            //        HttpSession session = request.getSession(true);
            //        // user always needs to update password from here, if successful
            //        session.setAttribute("resetCode", code);
            //        // TODO build url
            //        session.setAttribute(RequestAwareAuthenticationSuccessHandler.SAVED_REQUEST,
            //                "/changepwd/" + providerId + "/" + account.getUuid());

            // build a request
            ResetKeyAuthenticationToken authenticationRequest = new ResetKeyAuthenticationToken(username, code);

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
        } catch (BadCredentialsException e) {
            throw new InternalAuthenticationException(null, null, null, "reset-key", e, e.getMessage());
        }
    }

    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return authenticationEntryPoint;
    }
}
