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

package it.smartcommunitylab.aac.oauth.auth;

import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.auth.LoginUrlRequestConverter;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.service.IdentityProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OAuth2IdpAwareLoginUrlConverter implements LoginUrlRequestConverter {

    public static final String IDP_PARAMETER_NAME = "idp_hint";

    private final IdentityProviderService providerService;
    private final IdentityProviderAuthorityService authorityService;

    public OAuth2IdpAwareLoginUrlConverter(
        IdentityProviderService providerService,
        IdentityProviderAuthorityService authorityService
    ) {
        Assert.notNull(providerService, "provider service is required");
        Assert.notNull(authorityService, "authority service is required");

        this.authorityService = authorityService;
        this.providerService = providerService;
    }

    @Override
    public String convert(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) {
        // check if idp hint via param
        String idpHint = null;
        if (request.getParameter(IDP_PARAMETER_NAME) != null) {
            idpHint = request.getParameter(IDP_PARAMETER_NAME);
        }

        // check if idp hint via attribute
        if (request.getAttribute(IDP_PARAMETER_NAME) != null) {
            idpHint = (String) request.getAttribute(IDP_PARAMETER_NAME);
        }

        // check if idp hint
        if (StringUtils.hasText(idpHint)) {
            // TODO check for idpHint == authorityId
            // needs discoverable realm either via path or via clientId
            try {
                ConfigurableIdentityProvider idp = providerService.getProvider(idpHint);
                // TODO check if active

                // fetch providers for given realm
                IdentityProvider<?, ?, ?, ?, ?> provider = authorityService
                    .getAuthority(idp.getAuthority())
                    .getProvider(idp.getProvider());
                if (provider == null) {
                    throw new NoSuchProviderException();
                }

                return provider.getAuthenticationUrl();
            } catch (NoSuchAuthorityException | NoSuchProviderException e) {
                // no valid response
                return null;
            }
        }

        // not found
        return null;
    }
}
