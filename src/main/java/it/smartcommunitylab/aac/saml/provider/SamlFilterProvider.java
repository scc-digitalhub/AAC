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

package it.smartcommunitylab.aac.saml.provider;

import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.saml.auth.Saml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.saml.auth.SamlMetadataFilter;
import it.smartcommunitylab.aac.saml.auth.SamlRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.saml.auth.SamlWebSsoAuthenticationFilter;
import it.smartcommunitylab.aac.saml.auth.SamlWebSsoAuthenticationRequestFilter;
import it.smartcommunitylab.aac.saml.auth.SerializableSaml2AuthenticationRequestContext;
import it.smartcommunitylab.aac.saml.service.HttpSessionSaml2AuthenticationRequestRepository;
import jakarta.servlet.Filter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestContext;
import org.springframework.util.Assert;

public class SamlFilterProvider implements FilterProvider {

    private final String authorityId;

    private final ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository;
    private final SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    private AuthenticationManager authManager;

    public SamlFilterProvider(
        String authorityId,
        SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
        ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository
    ) {
        Assert.hasText(authorityId, "authority can not be null or empty");
        Assert.notNull(registrationRepository, "registration repository is mandatory");
        Assert.notNull(relyingPartyRegistrationRepository, "relying party registration repository is mandatory");

        this.authorityId = authorityId;
        this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
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
        // build request repository bound to session
        Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> authenticationRequestRepository =
            new HttpSessionSaml2AuthenticationRequestRepository();

        // build filters
        SamlWebSsoAuthenticationRequestFilter requestFilter = new SamlWebSsoAuthenticationRequestFilter(
            authorityId,
            registrationRepository,
            relyingPartyRegistrationRepository,
            buildFilterUrl("authenticate/{registrationId}")
        );
        requestFilter.setAuthenticationRequestRepository(authenticationRequestRepository);

        SamlWebSsoAuthenticationFilter ssoFilter = new SamlWebSsoAuthenticationFilter(
            registrationRepository,
            relyingPartyRegistrationRepository,
            buildFilterUrl("sso/{registrationId}"),
            null
        );
        ssoFilter.setAuthenticationRequestRepository(authenticationRequestRepository);
        // TODO use custom success handler to support auth sagas (disabled for now)
        //        ssoFilter.setAuthenticationSuccessHandler(new RequestAwareAuthenticationSuccessHandler());

        SamlMetadataFilter metadataFilter = new SamlMetadataFilter(
            authorityId,
            relyingPartyRegistrationRepository,
            buildFilterUrl("metadata/{registrationId}")
        );

        if (authManager != null) {
            ssoFilter.setAuthenticationManager(authManager);
        }

        // build composite filterChain
        List<Filter> filters = new ArrayList<>();
        filters.add(metadataFilter);
        filters.add(requestFilter);
        filters.add(ssoFilter);

        return filters;
    }

    @Override
    public Collection<Filter> getChainFilters() {
        return null;
    }

    @Override
    public Collection<String> getCorsIgnoringAntMatchers() {
        return Arrays
            .asList(NO_CORS_ENDPOINTS)
            .stream()
            .map(a -> "/auth/" + authorityId + "/" + a)
            .collect(Collectors.toList());
    }

    private String buildFilterUrl(String action) {
        // always use same path building logic for saml
        return "/auth/" + authorityId + "/" + action;
    }

    private static String[] NO_CORS_ENDPOINTS = { "authenticate/**", "sso/**" };
}
