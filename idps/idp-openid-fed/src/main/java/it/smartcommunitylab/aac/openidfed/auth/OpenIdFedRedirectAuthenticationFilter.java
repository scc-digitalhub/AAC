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

package it.smartcommunitylab.aac.openidfed.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.oidc.events.OAuth2AuthorizationRequestEvent;
import it.smartcommunitylab.aac.openidfed.OpenIdFedIdentityAuthority;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

/*
 * Build authorization request for OpenIdFed
 *
 * Note: use a custom filter to make sure oncePerRequest uses our name to check execution
 */

public class OpenIdFedRedirectAuthenticationFilter
    extends OncePerRequestFilter
    implements ApplicationEventPublisherAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String DEFAULT_FILTER_URI = OpenIdFedIdentityAuthority.AUTHORITY_URL + "authorize/{providerId}";

    private final RequestMatcher requestMatcher;
    private final RedirectStrategy authorizationRedirectStrategy = new DefaultRedirectStrategy();
    private OAuth2AuthorizationRequestResolver authorizationRequestResolver;

    private AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository =
        new HttpSessionOAuth2AuthorizationRequestRepository();

    private final String authority;
    private final ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository;

    private ApplicationEventPublisher eventPublisher;

    public OpenIdFedRedirectAuthenticationFilter(
        ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository
    ) {
        this(SystemKeys.AUTHORITY_OPENIDFED, registrationRepository, DEFAULT_FILTER_URI);
    }

    public OpenIdFedRedirectAuthenticationFilter(
        String authority,
        ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository,
        String filterProcessingUrl
    ) {
        Assert.hasText(authority, "authority can not be null or empty");
        Assert.notNull(registrationRepository, "provider registration repository cannot be null");

        this.authority = authority;
        this.registrationRepository = registrationRepository;
        this.authorizationRequestResolver = new OpenIdFedOAuth2AuthorizationRequestResolver(
            registrationRepository,
            filterProcessingUrl
        );
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl + "/**");
    }

    @Nullable
    protected String getFilterName() {
        return getClass().getName() + "." + authority;
    }

    public void setAuthorizationRequestRepository(
        AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository
    ) {
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (!requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            logger.debug("resolving authorization request from http request");
            OAuth2AuthorizationRequest authorizationRequest = this.authorizationRequestResolver.resolve(request);
            if (authorizationRequest == null) {
                logger.debug("error resolving authorization request from http request");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            //resolve provider
            String providerId = resolveProviderId(request);
            OpenIdFedIdentityProviderConfig config = registrationRepository.findByProviderId(providerId);
            if (config == null) {
                logger.error("error retrieving provider for registration {}", String.valueOf(providerId));
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            //save
            logger.debug(
                "persisting authorization request with context under key: {}",
                authorizationRequest.getState()
            );
            authorizationRequestRepository.saveAuthorizationRequest(authorizationRequest, request, response);

            //publish event
            if (eventPublisher != null) {
                logger.debug("publish event for authorization request {}", authorizationRequest.getState());

                eventPublisher.publishEvent(
                    new OAuth2AuthorizationRequestEvent(
                        authority,
                        config.getProvider(),
                        config.getRealm(),
                        authorizationRequest
                    )
                );
            }

            //send redirect to client
            logger.debug("send redirect to client for authorization request {}", authorizationRequest.getState());
            if (logger.isTraceEnabled()) {
                logger.trace("redirectUri: {}", authorizationRequest.getAuthorizationRequestUri());
            }

            this.authorizationRedirectStrategy.sendRedirect(
                    request,
                    response,
                    authorizationRequest.getAuthorizationRequestUri()
                );
        } catch (Exception ex) {
            response.sendError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()
            );
        }
    }

    private String resolveProviderId(HttpServletRequest request) {
        if (this.requestMatcher.matches(request)) {
            return this.requestMatcher.matcher(request).getVariables().get("providerId");
        }
        return null;
    }
}
