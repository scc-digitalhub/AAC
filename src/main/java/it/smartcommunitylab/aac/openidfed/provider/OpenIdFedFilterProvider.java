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

package it.smartcommunitylab.aac.openidfed.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.openidfed.OpenIdFedIdentityAuthority;
import it.smartcommunitylab.aac.openidfed.auth.OpenIdFedLoginAuthenticationFilter;
import it.smartcommunitylab.aac.openidfed.auth.OpenIdFedMetadataFilter;
import it.smartcommunitylab.aac.openidfed.auth.OpenIdFedRedirectAuthenticationFilter;
import it.smartcommunitylab.aac.openidfed.auth.OpenIdFedResolverFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.servlet.Filter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.Assert;

public class OpenIdFedFilterProvider implements FilterProvider {

    private final String authorityId;
    private final ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository;

    private AuthenticationManager authManager;

    public OpenIdFedFilterProvider(ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository) {
        this(SystemKeys.AUTHORITY_OPENIDFED, registrationRepository);
    }

    public OpenIdFedFilterProvider(
        String authorityId,
        ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository
    ) {
        Assert.hasText(authorityId, "authority can not be null or empty");
        Assert.notNull(registrationRepository, "registration repository is mandatory");

        this.authorityId = authorityId;
        this.registrationRepository = registrationRepository;
    }

    public void setAuthManager(AuthenticationManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public String getAuthorityId() {
        return authorityId;
    }

    @Override
    public List<Filter> getAuthFilters() {
        // build filters bound to shared client + request repos
        AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository =
            new HttpSessionOAuth2AuthorizationRequestRepository();

        OpenIdFedMetadataFilter metadataFilter = new OpenIdFedMetadataFilter(
            authorityId,
            registrationRepository,
            buildFilterUrl("metadata/{providerId}")
        );

        OpenIdFedResolverFilter resolveFilter = new OpenIdFedResolverFilter(
            authorityId,
            registrationRepository,
            buildFilterUrl("resolve/{providerId}")
        );

        //        OAuth2AuthorizationRequestRedirectFilter redirectFilter = new OAuth2AuthorizationRequestRedirectFilter(
        //                clientRegistrationRepository, OpenIdFedIdentityAuthority.AUTHORITY_URL + "authorize");
        OpenIdFedRedirectAuthenticationFilter redirectFilter = new OpenIdFedRedirectAuthenticationFilter(
            authorityId,
            registrationRepository,
            buildFilterUrl("authorize")
        );
        redirectFilter.setAuthorizationRequestRepository(authorizationRequestRepository);

        OpenIdFedLoginAuthenticationFilter loginFilter = new OpenIdFedLoginAuthenticationFilter(
            authorityId,
            registrationRepository,
            buildFilterUrl("login/{providerId}"),
            null
        );
        loginFilter.setAuthorizationRequestRepository(authorizationRequestRepository);
        // TODO use custom success handler to support auth sagas (disabled for now)
        //        loginFilter.setAuthenticationSuccessHandler(new RequestAwareAuthenticationSuccessHandler();

        if (authManager != null) {
            loginFilter.setAuthenticationManager(authManager);
        }

        // build composite filterChain
        List<Filter> filters = new ArrayList<>();
        filters.add(metadataFilter);
        filters.add(resolveFilter);
        filters.add(loginFilter);
        filters.add(redirectFilter);

        return filters;
    }

    @Override
    public Collection<Filter> getChainFilters() {
        return null;
    }

    @Override
    public Collection<String> getCorsIgnoringAntMatchers() {
        return Arrays.asList(NO_CORS_ENDPOINTS);
    }

    private String buildFilterUrl(String action) {
        // always use same path building logic for oidc
        return "/auth/" + authorityId + "/" + action;
    }

    private static String[] NO_CORS_ENDPOINTS = { OpenIdFedIdentityAuthority.AUTHORITY_URL + "login/**" };
}
