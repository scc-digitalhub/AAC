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

package it.smartcommunitylab.aac.oidc.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.oidc.OIDCIdentityAuthority;
import it.smartcommunitylab.aac.oidc.provider.OIDCIdentityProviderConfig;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.util.Assert;

/*
 * Build authorization request for external OIDC providers and redirects user-agent
 */

public class OIDCRedirectAuthenticationFilter extends OAuth2AuthorizationRequestRedirectFilter {

    public static final String DEFAULT_FILTER_URI = OIDCIdentityAuthority.AUTHORITY_URL + "authorize";

    private final String authorityId;

    //    // we need to load client registration
    //    private final ClientRegistrationRepository clientRegistrationRepository;
    //    private final ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository;

    public OIDCRedirectAuthenticationFilter(
        ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository,
        ClientRegistrationRepository clientRegistrationRepository
    ) {
        this(SystemKeys.AUTHORITY_OIDC, registrationRepository, clientRegistrationRepository, DEFAULT_FILTER_URI);
    }

    public OIDCRedirectAuthenticationFilter(
        String authority,
        ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository,
        ClientRegistrationRepository clientRegistrationRepository,
        String filterProcessesUrl
    ) {
        // set PKCE aware request resolver to enforce pkce for both public and private
        // clients
        super(
            new ExtendedAuthorizationRequestResolver(
                new PKCEAwareOAuth2AuthorizationRequestResolver(
                    registrationRepository,
                    clientRegistrationRepository,
                    filterProcessesUrl
                ),
                registrationRepository
            )
        );
        Assert.hasText(authority, "authority can not be null or empty");
        Assert.notNull(registrationRepository, "provider registration repository cannot be null");
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");

        this.authorityId = authority;
        //        this.registrationRepository = registrationRepository;
        //        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Nullable
    protected String getFilterName() {
        return getClass().getName() + "." + authorityId;
    }
}
