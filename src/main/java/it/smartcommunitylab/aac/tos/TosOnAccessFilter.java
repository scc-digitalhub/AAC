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

package it.smartcommunitylab.aac.tos;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.MyUserManager;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.RealmAwareAuthenticationEntryPoint;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.tos.controller.TosController;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

public class TosOnAccessFilter extends OncePerRequestFilter {

    private final RequestMatcher termsManagedRequestMatcher = new AntPathRequestMatcher("/terms/**");
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

    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private RequestMatcher requestMatcher;

    private final RealmService realmService;
    private final UserService userService;

    private static final String TOS_APRROVED = "Approve";
    private static final String TOS_REFUSED = "Refuse";
    private static final String TOS_STATUS = "termsStatus";

    public TosOnAccessFilter(RealmService realmService, UserService userService) {
        // init request cache as store
        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        this.requestCache = cache;
        this.realmService = realmService;
        this.userService = userService;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        if (requestMatcher.matches(request) && requiresProcessing() && !termsManagedRequestMatcher.matches(request)) {
            logger.trace("process request for {}", request.getRequestURI());
            UserAuthentication userAuth = (UserAuthentication) SecurityContextHolder.getContext().getAuthentication();
            ExtendedAuthenticationToken token = CollectionUtils.firstElement(userAuth.getAuthentications());
            String realm = token.getRealm();
            // check if realm is obliged to tos.
            Realm realmEntity = realmService.findRealm(realm);
            User user = userService.findUser(userAuth.getSubjectId());

            if (user != null && realmEntity != null && realmEntity.getTosConfiguration().isEnableTOS()) {
                // Refuse.
                if (user.getTosAccepted() != null && !user.isTosAccepted()) {
                    // check session is empty
                    if (
                        request.getSession().getAttribute(TOS_STATUS) != null &&
                        request.getSession().getAttribute(TOS_STATUS).equals(TOS_REFUSED)
                    ) {
                        // clear session.
                        SecurityContextHolder.clearContext();
                        request.getSession().removeAttribute(TOS_STATUS);
                    }

                    String refuseUrl = TosController.TOS_TERMS_REJECT;
                    this.logger.debug("Redirect to {}", refuseUrl);
                    request.setAttribute(RealmAwareAuthenticationEntryPoint.REALM_URI_VARIABLE_NAME, user.getRealm());
                    this.redirectStrategy.sendRedirect(request, response, refuseUrl);
                    return;
                } else if (user.getTosAccepted() != null && user.isTosAccepted()) { // Approve
                    // if accepted in current session
                    if (
                        request.getSession().getAttribute(TOS_STATUS) != null &&
                        request.getSession().getAttribute(TOS_STATUS).equals(TOS_APRROVED)
                    ) {
                        SavedRequest savedRequest = this.requestCache.getRequest(request, response);
                        if (savedRequest != null) {
                            logger.debug("restore request from cache");
                            this.requestCache.removeRequest(request, response);
                            request.getSession().removeAttribute(TOS_STATUS);
                            this.redirectStrategy.sendRedirect(request, response, savedRequest.getRedirectUrl());
                            return;
                        }
                    }
                } else { // default (Null)
                    String targetUrl = TosController.TOS_TERMS;
                    this.requestCache.saveRequest(request, response);
                    this.logger.debug("Redirect to {}", targetUrl);
                    this.redirectStrategy.sendRedirect(request, response, targetUrl);
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private boolean requiresProcessing() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof UserAuthentication)) {
            return false;
        }

        UserAuthentication userAuth = (UserAuthentication) SecurityContextHolder.getContext().getAuthentication();
        ExtendedAuthenticationToken token = CollectionUtils.firstElement(userAuth.getAuthentications());

        if (token == null) {
            logger.error("empty token on authentication success");
            return false;
        }

        String realm = token.getRealm();

        boolean require = false;
        if (realm.equalsIgnoreCase(SystemKeys.REALM_GLOBAL) || realm.equalsIgnoreCase(SystemKeys.REALM_SYSTEM)) {
            require = false;
        } else {
            require = true;
        }

        return require;
    }
}
