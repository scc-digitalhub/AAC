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

package it.smartcommunitylab.aac.core.auth;

import java.io.IOException;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ExpiredUserAuthenticationFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        // check if auth is present and valid
        UserAuthentication userAuth = getUserAuthentication();
        if (userAuth != null) {
            logger.debug("check user authentication for expired tokens for " + userAuth.getSubjectId());
            // prune expired
            // TODO ask reauth for these
            Set<ExtendedAuthenticationToken> tokens = userAuth.getAuthentications();
            for (ExtendedAuthenticationToken token : tokens) {
                if (token.isExpired()) {
                    logger.debug("purge expired token for " + token.getName());
                    userAuth.eraseAuthentication(token);
                }
            }

            if (userAuth.getAuthentications().isEmpty() || userAuth.isExpired()) {
                logger.debug("user authentication has no valid tokens for " + userAuth.getSubjectId());

                // not valid, clear context to force logout+reauth
                SecurityContextHolder.clearContext();

                // set realm to feed authEntryPoint
                request.setAttribute("realm", userAuth.getRealm());
                // set hint for provider to last one
                // TODO
            }
        }

        // always continue processing
        chain.doFilter(request, response);
        return;
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
}
