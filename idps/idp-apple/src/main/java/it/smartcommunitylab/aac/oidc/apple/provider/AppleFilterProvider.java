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

package it.smartcommunitylab.aac.oidc.apple.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.oidc.apple.AppleIdentityAuthority;
import it.smartcommunitylab.aac.oidc.apple.auth.AppleClientRegistrationRepository;
import it.smartcommunitylab.aac.oidc.apple.auth.AppleLoginAuthenticationFilter;
import it.smartcommunitylab.aac.oidc.apple.auth.AppleRedirectAuthenticationFilter;
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

public class AppleFilterProvider implements FilterProvider {

    private final AppleClientRegistrationRepository clientRegistrationRepository;
    private final ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository;

    private AuthenticationManager authManager;

    public AppleFilterProvider(
        AppleClientRegistrationRepository clientRegistrationRepository,
        ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository
    ) {
        Assert.notNull(registrationRepository, "registration repository is mandatory");
        Assert.notNull(clientRegistrationRepository, "client registration repository is mandatory");

        this.clientRegistrationRepository = clientRegistrationRepository;
        this.registrationRepository = registrationRepository;
    }

    public void setAuthManager(AuthenticationManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_APPLE;
    }

    @Override
    public List<Filter> getAuthFilters() {
        // build filters bound to shared client + request repos
        AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository =
            new HttpSessionOAuth2AuthorizationRequestRepository();

        //        OAuth2AuthorizationRequestRedirectFilter redirectFilter = new OAuth2AuthorizationRequestRedirectFilter(
        //                clientRegistrationRepository, AppleIdentityAuthority.AUTHORITY_URL + "authorize");
        AppleRedirectAuthenticationFilter redirectFilter = new AppleRedirectAuthenticationFilter(
            registrationRepository,
            clientRegistrationRepository
        );
        redirectFilter.setAuthorizationRequestRepository(authorizationRequestRepository);

        AppleLoginAuthenticationFilter loginFilter = new AppleLoginAuthenticationFilter(
            registrationRepository,
            clientRegistrationRepository
        );

        loginFilter.setAuthorizationRequestRepository(authorizationRequestRepository);
        // TODO use custom success handler to support auth sagas (disabled for now)
        //        loginFilter.setAuthenticationSuccessHandler(new RequestAwareAuthenticationSuccessHandler();

        if (authManager != null) {
            loginFilter.setAuthenticationManager(authManager);
        }

        // build composite filterChain
        List<Filter> filters = new ArrayList<>();
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

    private static String[] NO_CORS_ENDPOINTS = { AppleIdentityAuthority.AUTHORITY_URL + "login/**" };
}
