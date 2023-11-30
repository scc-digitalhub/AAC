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
import it.smartcommunitylab.aac.oidc.auth.ExtendedAuthorizationRequestResolver;
import it.smartcommunitylab.aac.openidfed.OpenIdFedIdentityAuthority;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.util.Assert;

/*
 * Build authorization request for OpenIdFed
 *
 * Note: use a custom filter to make sure oncePerRequest uses our name to check execution
 */

public class OpenIdFedRedirectAuthenticationFilter extends OAuth2AuthorizationRequestRedirectFilter {

    public static final String DEFAULT_FILTER_URI = OpenIdFedIdentityAuthority.AUTHORITY_URL + "authorize";

    private final String authorityId;

    public OpenIdFedRedirectAuthenticationFilter(
        ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository,
        ClientRegistrationRepository clientRegistrationRepository
    ) {
        this(SystemKeys.AUTHORITY_OPENIDFED, registrationRepository, clientRegistrationRepository, DEFAULT_FILTER_URI);
    }

    public OpenIdFedRedirectAuthenticationFilter(
        String authority,
        ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository,
        ClientRegistrationRepository clientRegistrationRepository,
        String filterProcessesUrl
    ) {
        // set openid fed request resolver
        super(
            new OpenIdFedOAuth2AuthorizationRequestResolver(
                registrationRepository,
                clientRegistrationRepository,
                filterProcessesUrl
            )
        );
        Assert.hasText(authority, "authority can not be null or empty");
        Assert.notNull(registrationRepository, "provider registration repository cannot be null");
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");

        this.authorityId = authority;
    }

    @Nullable
    protected String getFilterName() {
        return getClass().getName() + "." + authorityId;
    }
}
