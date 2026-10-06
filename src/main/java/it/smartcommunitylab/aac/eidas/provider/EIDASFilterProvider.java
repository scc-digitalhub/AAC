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

package it.smartcommunitylab.aac.eidas.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.eidas.EIDASIdentityAuthority;
import it.smartcommunitylab.aac.eidas.auth.EIDASMetadataFilter;
import it.smartcommunitylab.aac.eidas.auth.EIDASWebSsoAuthenticationFilter;
import it.smartcommunitylab.aac.eidas.auth.EIDASWebSsoAuthenticationRequestFilter;
import it.smartcommunitylab.aac.saml.auth.Saml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.saml.auth.SerializableSaml2AuthenticationRequestContext;
import it.smartcommunitylab.aac.saml.service.HttpSessionSaml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.spid.auth.SpidMetadataRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.spid.auth.SpidRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.spid.provider.SpidFilterProvider;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.util.Assert;

/*
 * The SPID filter provider, moved onto the eidas authority: same three filters, same wiring, under
 * /auth/eidas/ and with eidas names.
 *
 *  a. EIDASWebSsoAuthenticationRequestFilter builds and sends the AuthnRequest (the SPID filter, renamed)
 *  b. EIDASWebSsoAuthenticationFilter collects the response and routes it to the eidas authority
 *  c. EIDASMetadataFilter publishes the SP metadata (the SPID filter, renamed)
 *
 * a. and c. add nothing to the SPID filters but their name: AuthConfig chains the filters of every
 * authority together, and OncePerRequestFilter tells instances apart by getFilterName(). Reusing the
 * SPID classes as they are would put two instances of the same name in the chain and the second one -
 * the SPID one - would be skipped (404 on /auth/spid/authenticate/ and /auth/spid/metadata/).
 *
 * Why a subclass, and why it still builds its own filters. SpidFilterProvider keeps everything private
 * (authority id, repositories, event publisher, authentication manager) and instantiates the SPID
 * filters inside getAuthFilters(): there is no hook to swap a filter class or the authority. So the
 * subclass keeps its own references to what the constructor receives, intercepts the two setters and
 * rebuilds the list with the eidas classes. What is inherited is the contract and the constructor
 * shape, which is what keeps this class aligned when SpidFilterProvider evolves (a fourth filter, a
 * new endpoint in the CORS list): the change lands here as an addition, not as a divergence. Should
 * SPID expose protected factory methods for its filters, this class shrinks to those overrides.
 */
public class EIDASFilterProvider extends SpidFilterProvider {

    private static final String[] NO_CORS_ENDPOINTS = { "authenticate/**", "sso/**" }; // must match filters endpoints

    private final ProviderConfigRepository<EIDASIdentityProviderConfig> providerConfigRepository;
    private final ProviderConfigRepository<SpidIdentityProviderConfig> spidConfigRepository;
    private final SpidRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;
    private final SpidMetadataRelyingPartyRegistrationRepository metadataRelyingPartyRegistrationRepository;

    // the parent keeps its own copies private: mirrored here for the filters we build
    private ApplicationEventPublisher eventPublisher;
    private AuthenticationManager authManager;

    public EIDASFilterProvider(
        SpidRelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
        SpidMetadataRelyingPartyRegistrationRepository metadataRelyingPartyRegistrationRepository,
        ProviderConfigRepository<EIDASIdentityProviderConfig> providerConfigRepository
    ) {
        super(relyingPartyRegistrationRepository, metadataRelyingPartyRegistrationRepository, asSpidRepository(providerConfigRepository));

        this.providerConfigRepository = providerConfigRepository;
        this.spidConfigRepository = asSpidRepository(providerConfigRepository);
        this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
        this.metadataRelyingPartyRegistrationRepository = metadataRelyingPartyRegistrationRepository;
    }

    /*
     * the spid filters read configs from a spid registry: EIDASIdentityProviderConfig is already one,
     * but java generics are invariant, hence the identity translation
     */
    private static ProviderConfigRepository<SpidIdentityProviderConfig> asSpidRepository(
        ProviderConfigRepository<EIDASIdentityProviderConfig> providerConfigRepository
    ) {
        Assert.notNull(providerConfigRepository, "registration repository is mandatory");
        TranslatorProviderConfigRepository<EIDASIdentityProviderConfig, SpidIdentityProviderConfig> spidConfigs =
            new TranslatorProviderConfigRepository<>(providerConfigRepository);
        spidConfigs.setConverter(config -> config);
        return spidConfigs;
    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_EIDAS;
    }

    @Override
    public Collection<Filter> getAuthFilters() {
        List<Filter> filters = new ArrayList<>();

        // build request repository bound to session.
        // NOTE: the default session attribute is mandatory here. Saml2AuthenticationTokenConverter
        // reloads the authentication request from a repository it builds itself, on the default
        // attribute: with a custom key the token would carry no request and the response would fail
        // validation with invalid_in_response_to.
        Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> authenticationRequestRepository =
            new HttpSessionSaml2AuthenticationRequestRepository();

        // the spid request filter is authority agnostic, it only needs our endpoint and registry (and its own name)
        EIDASWebSsoAuthenticationRequestFilter requestFilter = new EIDASWebSsoAuthenticationRequestFilter(
            spidConfigRepository,
            relyingPartyRegistrationRepository,
            buildFilterUrl("authenticate/{registrationId}")
        );
        requestFilter.setApplicationEventPublisher(eventPublisher);
        requestFilter.setAuthenticationRequestRepository(authenticationRequestRepository);

        EIDASWebSsoAuthenticationFilter ssoFilter = new EIDASWebSsoAuthenticationFilter(
            providerConfigRepository,
            relyingPartyRegistrationRepository,
            buildFilterUrl("sso/{registrationId}"),
            null
        );
        ssoFilter.setApplicationEventPublisher(eventPublisher);
        ssoFilter.setAuthenticationRequestRepository(authenticationRequestRepository);
        if (this.authManager != null) {
            ssoFilter.setAuthenticationManager(authManager);
        }

        EIDASMetadataFilter metadataFilter = new EIDASMetadataFilter(
            spidConfigRepository,
            metadataRelyingPartyRegistrationRepository,
            buildFilterUrl("metadata/{registrationId}")
        );

        filters.add(requestFilter);
        filters.add(ssoFilter);
        filters.add(metadataFilter);

        return filters;
    }

    /* the parent computes these on its own, private, authority id */
    @Override
    public Collection<String> getCorsIgnoringAntMatchers() {
        return Arrays.stream(NO_CORS_ENDPOINTS).map(this::buildFilterUrl).collect(Collectors.toList());
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        super.setApplicationEventPublisher(applicationEventPublisher);
        this.eventPublisher = applicationEventPublisher;
    }

    @Override
    public void setAuthManager(AuthenticationManager authManager) {
        super.setAuthManager(authManager);
        this.authManager = authManager;
    }

    private String buildFilterUrl(String action) {
        return EIDASIdentityAuthority.AUTHORITY_URL + action;
    }
}
