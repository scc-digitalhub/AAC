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
		if (requestMatcher.matches(request) && requiresProcessing(request)
				&& !termsManagedRequestMatcher.matches(request)) {
			logger.trace("process request for {}", request.getRequestURI());
			UserAuthentication userAuth = (UserAuthentication) SecurityContextHolder.getContext().getAuthentication();
			ExtendedAuthenticationToken token = CollectionUtils.firstElement(userAuth.getAuthentications());

			if (token == null) {
				logger.error("empty token on authentication success");
				return;
			}

			String realm = token.getRealm();

			// check if realm is mutable
			if (!realm.equalsIgnoreCase(SystemKeys.REALM_GLOBAL) && !realm.equalsIgnoreCase(SystemKeys.REALM_SYSTEM)) {
				// check if realm is obliged to tos.
				try {
					Realm realmEntity = realmService.findRealm(realm);
					if (realmEntity.getTosConfiguration().getConfiguration().containsKey("enableTOS")
							&& (boolean) realmEntity.getTosConfiguration().getConfiguration().get("enableTOS")) {
						User user = userService.getUser(userAuth.getSubjectId());
						
						if (user.getTosAccepted() != null) {
							// check if rejected and session is empty
							if (!user.getTosAccepted()) {
								if (request.getSession().getAttribute("termsStatus") != null
										&& request.getSession().getAttribute("termsStatus").equals(TOS_REFUSED)) {
									// clear session.
									SecurityContextHolder.clearContext();
									request.getSession().removeAttribute("termsStatus");
								}
								String refuseUrl = "/terms/refuse";
								this.logger.debug("Redirect to {}", refuseUrl);
								request.setAttribute(RealmAwareAuthenticationEntryPoint.REALM_URI_VARIABLE_NAME,
										user.getRealm());
								this.redirectStrategy.sendRedirect(request, response, refuseUrl);
								return;
							} else if (request.getSession().getAttribute("termsStatus") != null // if accepted in current session
									&& request.getSession().getAttribute("termsStatus").equals(TOS_APRROVED)) {
								SavedRequest savedRequest = this.requestCache.getRequest(request, response);
								if (savedRequest != null) {
									logger.debug("restore request from cache");
									this.requestCache.removeRequest(request, response);
									request.getSession().removeAttribute("termsStatus");
									this.redirectStrategy.sendRedirect(request, response,
											savedRequest.getRedirectUrl());
									return;
								}
							} else { // approved in past.
								chain.doFilter(request, response);
								return;
							}
						} else { // default
							String targetUrl = "/terms";
							this.requestCache.saveRequest(request, response);
							this.logger.debug("Redirect to {}", targetUrl);
							this.redirectStrategy.sendRedirect(request, response, targetUrl);
						}
					} else {
						chain.doFilter(request, response);
						return;
					}
				} catch (NoSuchUserException e) {
					e.printStackTrace();
				}
			} else {
				chain.doFilter(request, response);
				return;

			}
		} else {
			chain.doFilter(request, response);
			return;
		}
	}

            
            
            
            // check if user already accepted it or rejected,
            // From filter redirect to error page which is inside controller.
            // realm tos enabled, if laready accepted go on 
            // if rejected error page.
            // if it is something in this session restore the requrest, if there is session around, if there is SET flage in DB and there is flag in the SESSION
            // so you need to restore the save request. SAVE REQUEST is inside filter.
            // if it is rejected, i show 
            
//            ON NULL USER DOES NOT MAKE ANY CHOICE, KEEP SAVING and SET IN THE FORM
            // IF IT IS REFUSE YOU HAVE TO CLEAR THE CONTEXT
            // if it is alaredy accepted or reject and session is empty just go on.
            
            // we will have anohter filter to check if accept or reject here we handle reject only in this session.
            // THIS filter will act only if there is something inside session.
            // filter needs to only check the session and destroy 
            
            // controller manage objs and filter manage session. deleting the session will get the user to error page
            
            // if nothing in session, nothing will happen
            
            // if accepted and there is flag in the session, restore
            // if reject and there is a flag, clear it and redirect
            // chain of filter
            // acutal chain not outside something , you need not to return but to doFilter(), always do the doFilter
            // reverse the logic 
            // if everhting is not null do something else you alwasy call chain.
            // it intercept every request.

       
//    }

    private boolean requiresProcessing(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth instanceof UserAuthentication)) {
            return false;
        }
        return true;
    }
}
