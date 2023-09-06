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

package it.smartcommunitylab.aac.webauthn.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.RequestAwareAuthenticationSuccessHandler;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAuthenticationFilter;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnLoginRpService;
import it.smartcommunitylab.aac.webauthn.store.WebAuthnAssertionRequestStore;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.servlet.Filter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.util.Assert;

public class WebAuthnIdentityFilterProvider implements FilterProvider {

    private final WebAuthnLoginRpService rpService;

    private final ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository;
    private final WebAuthnAssertionRequestStore requestStore;

    private AuthenticationManager authManager;

    public WebAuthnIdentityFilterProvider(
        WebAuthnLoginRpService rpService,
        ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository,
        WebAuthnAssertionRequestStore requestStore
    ) {
        Assert.notNull(rpService, "webauthn rp service is mandatory");
        Assert.notNull(registrationRepository, "registration repository is mandatory");
        Assert.notNull(requestStore, "webauthn request store is mandatory");

        this.rpService = rpService;

        this.registrationRepository = registrationRepository;
        this.requestStore = requestStore;
    }

    public void setAuthManager(AuthenticationManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_WEBAUTHN;
    }

    @Override
    public List<Filter> getAuthFilters() {
        WebAuthnAuthenticationFilter loginFilter = new WebAuthnAuthenticationFilter(
            rpService,
            requestStore,
            registrationRepository
        );
        loginFilter.setAuthenticationSuccessHandler(successHandler());

        if (authManager != null) {
            loginFilter.setAuthenticationManager(authManager);
        }

        return Collections.singletonList(loginFilter);
    }

    @Override
    public Collection<Filter> getChainFilters() {
        return null;
    }

    @Override
    public Collection<String> getCorsIgnoringAntMatchers() {
        return Arrays.asList(NO_CORS_ENDPOINTS);
    }

    // TODO define in detail urls
    private static String[] NO_CORS_ENDPOINTS = { WebAuthnIdentityAuthority.AUTHORITY_URL + "**" };

    private RequestAwareAuthenticationSuccessHandler successHandler() {
        return new RequestAwareAuthenticationSuccessHandler();
    }
}
