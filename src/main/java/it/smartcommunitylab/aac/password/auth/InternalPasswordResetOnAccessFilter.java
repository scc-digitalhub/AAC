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

import it.smartcommunitylab.aac.core.auth.RealmAwareAuthenticationEntryPoint;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordEntity;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordEntityRepository;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.users.auth.UserAuthentication;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

public class InternalPasswordResetOnAccessFilter extends OncePerRequestFilter {

    static final String SAVED_REQUEST = "INTERNAL_PASSWORD_SAVED_REQUEST";
    static final String[] SKIP_URLS = {
        "/api/**",
        "/html/**",
        "/js/**",
        "/lib/**",
        "/fonts/**",
        "/italia/**",
        "/i18n/**",
    };
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RequestCache requestCache;
    private final RequestMatcher changeRequestMatcher = new AntPathRequestMatcher("/changepwd/**");

    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private RequestMatcher requestMatcher;
    private boolean logoutAfterReset = true;

    private final ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository;
    //TODO replace with proper service
    private final InternalUserPasswordEntityRepository passwordRepository;

    public InternalPasswordResetOnAccessFilter(
        InternalUserPasswordEntityRepository passwordRepository,
        ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository
    ) {
        Assert.notNull(passwordRepository, "password repository is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository cannot be null");

        this.passwordRepository = passwordRepository;
        this.registrationRepository = registrationRepository;

        // init request cache as store
        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        cache.setSessionAttrName(SAVED_REQUEST);
        this.requestCache = cache;

        // build request matcher
        // by default skip static + api requests
        this.requestMatcher = buildRequestMatcher();
    }

    private RequestMatcher buildRequestMatcher() {
        List<RequestMatcher> antMatchers = Arrays
            .stream(SKIP_URLS)
            .map(u -> new AntPathRequestMatcher(u))
            .collect(Collectors.toList());

        return new NegatedRequestMatcher(new OrRequestMatcher(antMatchers));
    }

    public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
        this.redirectStrategy = redirectStrategy;
    }

    public void setRequestMatcher(RequestMatcher requestMatcher) {
        this.requestMatcher = requestMatcher;
    }

    public void setLogoutAfterReset(boolean logoutAfterReset) {
        this.logoutAfterReset = logoutAfterReset;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        if (requestMatcher.matches(request) && requiresProcessing(request) && !changeRequestMatcher.matches(request)) {
            boolean requireChange = false;
            String targetUrl = null;
            String realm = null;

            logger.trace("process request for {}", request.getRequestURI());

            // fetch user auth and extract reset key tokens
            UserAuthentication userAuth = (UserAuthentication) SecurityContextHolder.getContext().getAuthentication();
            Set<ResetKeyAuthenticationToken> resetTokens = userAuth
                .getAuthentications()
                .stream()
                .filter(e -> ResetKeyAuthenticationToken.class.isInstance(e.getToken()))
                .map(e -> (ResetKeyAuthenticationToken) e.getToken())
                .collect(Collectors.toSet());

            // check if any token still requires change
            // TODO handle more than one
            ResetKeyAuthenticationToken token = resetTokens.iterator().next();
            InternalUserAccount account = token.getAccount();
            String providerId = account.getProvider();
            String userId = account.getUserId();
            realm = account.getRealm();

            // pick provider config to resolve repositoryId
            // TODO remove and include repositoryId in credentials embedded in auth token
            PasswordIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(providerId);
            if (providerConfig == null) {
                this.logger.error("Error fetching configuration for active provider");
                return;
            }

            String repositoryId = providerConfig.getRepositoryId();

            // check if account has already set a password
            // we look for an active password created *after* this login
            long deadline = userAuth.getCreatedAt().getEpochSecond() * 1000;
            InternalUserPasswordEntity pass =
                passwordRepository.findByRepositoryIdAndUserIdAndStatusOrderByCreateDateDesc(
                    repositoryId,
                    userId,
                    "active"
                );
            if (pass == null || pass.getCreateDate().getTime() < deadline) {
                // require change because we still lack a valid password for post-reset login
                targetUrl = "/changepwd/" + providerId + "/" + userId;
                requireChange = true;
            }

            if (requireChange && targetUrl != null) {
                // save request and redirect
                logger.debug("save request to cache");
                this.requestCache.saveRequest(request, response);

                if (response.isCommitted()) {
                    this.logger.debug("Did not redirect to {} since response already committed.", targetUrl);
                    return;
                }

                this.logger.debug("Redirect to {}", targetUrl);
                this.redirectStrategy.sendRedirect(request, response, targetUrl);
                return;
            } else {
                // TODO evaluate explicit confirm/removal of this reset key

                // check if logout is set
                if (logoutAfterReset) {
                    // clear context - will force login
                    this.logger.debug("logout user after reset");
                    SecurityContextHolder.clearContext();

                    // add user realm as attribute for login handler
                    // TODO set and read from session because 302 will break the request
                    request.setAttribute(RealmAwareAuthenticationEntryPoint.REALM_URI_VARIABLE_NAME, realm);
                }

                // check if we need to restore a request
                SavedRequest savedRequest = this.requestCache.getRequest(request, response);

                if (savedRequest != null) {
                    logger.debug("restore request from cache");
                    this.requestCache.removeRequest(request, response);
                    this.redirectStrategy.sendRedirect(request, response, savedRequest.getRedirectUrl());
                    return;
                }
            }
        }

        // continue processing
        chain.doFilter(request, response);
        return;
    }

    private boolean requiresProcessing(HttpServletRequest request) {
        // process only authenticated requests
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth instanceof UserAuthentication)) {
            return false;
        }

        // process only if there is a reset key
        return ((UserAuthentication) auth).getAuthentications()
            .stream()
            .anyMatch(e -> ResetKeyAuthenticationToken.class.isInstance(e.getToken()));
        //        // process only if there is a reset key or password token in context
        //        return ((UserAuthentication) auth).getAuthentications().stream()
        //                .anyMatch(e -> ResetKeyAuthenticationToken.class.isInstance(e.getToken())
        //                        || UsernamePasswordAuthenticationToken.class.isInstance(e.getToken()));
    }
}
