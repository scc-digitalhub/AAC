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

package it.smartcommunitylab.aac.oauth.auth;

import it.smartcommunitylab.aac.core.auth.ComposedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.ExtendedLoginUrlAuthenticationEntryPoint;
import it.smartcommunitylab.aac.core.auth.LoginUrlRequestConverter;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.oauth.client.OAuth2Client;
import it.smartcommunitylab.aac.oauth.model.PromptMode;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientService;
import it.smartcommunitylab.aac.oauth.store.AuthorizationRequestStore;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthorizationEndpointFilter extends OncePerRequestFilter {

    public static final String DEFAULT_FILTER_URI = "/oauth/authorize";

    private RequestCache requestCache = new HttpSessionRequestCache();

    private final RequestMatcher requestMatcher;
    private AuthenticationEntryPoint authenticationEntryPoint;

    // we need access to client
    private final OAuth2ClientService clientService;

    public AuthorizationEndpointFilter(
        OAuth2ClientService clientService,
        OAuth2ClientDetailsService clientDetailsService,
        AuthorizationRequestStore authorizationRequestStore
    ) {
        this(clientService, clientDetailsService, authorizationRequestStore, DEFAULT_FILTER_URI);
    }

    public AuthorizationEndpointFilter(
        OAuth2ClientService clientService,
        OAuth2ClientDetailsService clientDetailsService,
        AuthorizationRequestStore authorizationRequestStore,
        String filterProcessingUrl
    ) {
        Assert.notNull(clientService, "client service is required");
        Assert.hasText(filterProcessingUrl, "filter url can not be null or empty");
        this.clientService = clientService;
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);

        // build an auth entry point
        // TODO build a select account entrypoint
        LoginUrlRequestConverter clientAwareConverter = new OAuth2ClientAwareLoginUrlConverter(
            clientDetailsService,
            authorizationRequestStore,
            "/login"
        );
        ExtendedLoginUrlAuthenticationEntryPoint entryPoint = new ExtendedLoginUrlAuthenticationEntryPoint(
            "/login",
            clientAwareConverter
        );

        this.authenticationEntryPoint = entryPoint;
    }

    public void setAuthenticationEntryPoint(AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        if (requestMatcher.matches(request) && requiresProcessing(request)) {
            // fetch params
            String clientId = request.getParameter("client_id");
            Set<String> prompt = delimitedStringToSet(request.getParameter("prompt"));
            String maxAge = request.getParameter("max_age");

            if (prompt.contains(PromptMode.LOGIN.getValue())) {
                // if prompt login redirect but keep session
                // we need to store request for after auth success
                // TODO remove prompt to avoid looping..
                // we need a way to bind reauth to requests, to avoid messing with context
                // disabled for now
                //                this.requestCache.saveRequest(request, response);
                //                this.authenticationEntryPoint.commence(request, response, null);
                //                return;
            }

            UserAuthentication userAuth = getUserAuthentication();

            if (maxAge != null) {
                // parse as long and compare to current auth
                try {
                    // in seconds
                    long max = Long.parseLong(maxAge);
                    if (max < userAuth.getAge()) {
                        // ask for relogin
                        this.requestCache.saveRequest(request, response);
                        this.authenticationEntryPoint.commence(request, response, null);
                        return;
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException();
                }
            }

            // load client
            OAuth2Client client = clientService.findClient(clientId);

            // we ignore errors, don't want to unauthenticated users on non existing
            // clients..
            if (client != null && userAuth != null) {
                // load idps
                Set<String> providers = client.getProviders();

                // user auth needs to contain at least one token from a client approved idp
                // TODO handle COMMON realm requests, for now only same realm is supported

                Set<String> userIdps = userAuth
                    .getAuthentications()
                    .stream()
                    .map(e -> e.getProvider())
                    .filter(p -> providers.contains(p))
                    .collect(Collectors.toSet());

                if (userIdps.isEmpty()) {
                    // user has no matching auth, we save request and ask for relogin
                    //                    SecurityContextHolder.clearContext();
                    this.requestCache.saveRequest(request, response);
                    this.authenticationEntryPoint.commence(request, response, null);
                    return;
                }
            }
        }

        // continue processing
        chain.doFilter(request, response);
        return;
    }

    private boolean requiresProcessing(HttpServletRequest request) {
        boolean hasParam = StringUtils.hasText(request.getParameter("client_id"));
        boolean hasAuth = !(getUserAuthentication() == null);

        return hasParam && hasAuth;
    }

    private UserAuthentication getUserAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UserAuthentication) {
            return (UserAuthentication) auth;
        }
        if (auth instanceof ComposedAuthenticationToken) {
            return ((ComposedAuthenticationToken) auth).getUserAuthentication();
        } else {
            return null;
        }
    }

    private Set<String> delimitedStringToSet(String str) {
        String[] tokens = StringUtils.delimitedListToStringArray(str, " ");
        return new LinkedHashSet<>(Arrays.asList(tokens));
    }
}
