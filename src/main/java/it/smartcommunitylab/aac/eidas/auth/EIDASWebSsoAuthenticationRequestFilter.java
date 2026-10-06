/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.eidas.auth;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.eidas.EIDASIdentityAuthority;
import it.smartcommunitylab.aac.spid.auth.SpidWebSsoAuthenticationRequestFilter;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;

/*
 * Builds and sends the AuthnRequest towards the eIDAS node: the SPID filter, unchanged, mounted under
 * /auth/eidas/authenticate/.
 *
 * Why a subclass and not the SPID class as it is: AuthConfig chains the filters of every authority in a single
 * CompositeFilter, and OncePerRequestFilter marks the request as "already filtered" with an attribute named after
 * getFilterName() - the class name, by default. Two instances of the same class in the same chain share the mark:
 * the first one (whichever authority comes first) sets it even when its url does not match, and the second one is
 * skipped. With the SPID class reused as is, /auth/spid/authenticate/ would end in a 404 as soon as the eidas
 * authority is present. A name of its own keeps the two marks apart.
 */
public class EIDASWebSsoAuthenticationRequestFilter extends SpidWebSsoAuthenticationRequestFilter {

    public static final String DEFAULT_FILTER_URI = EIDASIdentityAuthority.AUTHORITY_URL + "authenticate/{registrationId}";

    public EIDASWebSsoAuthenticationRequestFilter(
        ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository
    ) {
        this(providerConfigRepository, relyingPartyRegistrationRepository, DEFAULT_FILTER_URI);
    }

    public EIDASWebSsoAuthenticationRequestFilter(
        ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
        String filterProcessingUrl
    ) {
        super(providerConfigRepository, relyingPartyRegistrationRepository, filterProcessingUrl);
    }

    @Override
    protected String getFilterName() {
        return getClass().getName() + "." + EIDASIdentityAuthority.AUTHORITY_URL;
    }
}
