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

package it.smartcommunitylab.aac.oidc.apple.auth;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.oidc.apple.AppleIdentityAuthority;
import it.smartcommunitylab.aac.oidc.apple.provider.AppleIdentityProviderConfig;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;

/*
 * Build authorization request for Apple
 *
 * Note: use a custom filter to make sure oncePerRequest uses our name to check execution
 */

public class AppleRedirectAuthenticationFilter extends OAuth2AuthorizationRequestRedirectFilter {

    public static final String DEFAULT_FILTER_URI = AppleIdentityAuthority.AUTHORITY_URL + "authorize";

    public AppleRedirectAuthenticationFilter(
        ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository,
        ClientRegistrationRepository clientRegistrationRepository
    ) {
        this(registrationRepository, clientRegistrationRepository, DEFAULT_FILTER_URI);
    }

    public AppleRedirectAuthenticationFilter(
        ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository,
        ClientRegistrationRepository clientRegistrationRepository,
        String filterProcessesUrl
    ) {
        super(clientRegistrationRepository, filterProcessesUrl);
    }
}
