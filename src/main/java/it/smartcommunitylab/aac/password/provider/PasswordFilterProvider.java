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

package it.smartcommunitylab.aac.password.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.core.auth.RequestAwareAuthenticationSuccessHandler;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.password.auth.ResetKeyAuthenticationFilter;
import it.smartcommunitylab.aac.password.auth.UsernamePasswordAuthenticationFilter;
import it.smartcommunitylab.aac.password.service.InternalPasswordJpaUserCredentialsService;
import jakarta.servlet.Filter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.util.Assert;

public class PasswordFilterProvider implements FilterProvider {

    private final ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository;
    private final UserAccountService<InternalUserAccount> userAccountService;
    private final InternalPasswordJpaUserCredentialsService userPasswordService;

    private AuthenticationManager authManager;

    public PasswordFilterProvider(
        UserAccountService<InternalUserAccount> userAccountService,
        InternalPasswordJpaUserCredentialsService userPasswordService,
        ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository
    ) {
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(userPasswordService, "password service is mandatory");
        Assert.notNull(registrationRepository, "registration repository is mandatory");

        this.userAccountService = userAccountService;
        this.userPasswordService = userPasswordService;
        this.registrationRepository = registrationRepository;
    }

    public void setAuthManager(AuthenticationManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_PASSWORD;
    }

    @Override
    public List<Filter> getAuthFilters() {
        // build auth filters for user+password and resetKey
        UsernamePasswordAuthenticationFilter loginFilter = new UsernamePasswordAuthenticationFilter(
            userAccountService,
            userPasswordService,
            registrationRepository
        );
        loginFilter.setAuthenticationSuccessHandler(successHandler());

        ResetKeyAuthenticationFilter resetKeyFilter = new ResetKeyAuthenticationFilter(
            userAccountService,
            userPasswordService,
            registrationRepository
        );
        resetKeyFilter.setAuthenticationSuccessHandler(successHandler());

        if (authManager != null) {
            loginFilter.setAuthenticationManager(authManager);
            resetKeyFilter.setAuthenticationManager(authManager);
        }

        // build composite filterChain
        List<Filter> filters = new ArrayList<>();
        filters.add(loginFilter);
        filters.add(resetKeyFilter);

        return filters;
    }

    @Override
    public Collection<Filter> getChainFilters() {
        // TODO build chain filter to check password set/expire/reset etc
        return null;
    }

    @Override
    public Collection<String> getCorsIgnoringAntMatchers() {
        return Arrays.asList(NO_CORS_ENDPOINTS);
    }

    private static String[] NO_CORS_ENDPOINTS = {};

    private RequestAwareAuthenticationSuccessHandler successHandler() {
        return new RequestAwareAuthenticationSuccessHandler();
    }
}
