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

package it.smartcommunitylab.aac.clients;

import it.smartcommunitylab.aac.clients.auth.ClientAuthentication;
import it.smartcommunitylab.aac.clients.auth.ClientAuthenticationProvider;
import it.smartcommunitylab.aac.clients.service.ClientDetailsService;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.ClientDetails;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;

/*
 * Authentication manager for client
 *
 * handles authentication process via authentication providers resolving authTokens, and
 * enhances the result with a complete ClientAuthenticationToken with client details
 */
public class ClientAuthenticationManager implements AuthenticationManager, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ClientDetailsService clientService;
    private List<ClientAuthenticationProvider> providers = Collections.emptyList();

    public ClientAuthenticationManager(ClientAuthenticationProvider... providers) {
        this(Arrays.asList(providers));
    }

    public ClientAuthenticationManager(List<ClientAuthenticationProvider> providers) {
        this.providers = providers;
    }

    public void setClientService(ClientDetailsService clientService) {
        this.clientService = clientService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        logger.debug("process authentication for {}", String.valueOf(authentication.getName()));

        if (!(authentication instanceof ClientAuthentication)) {
            logger.error("invalid authentication class: {}", authentication.getClass().getName());
            throw new AuthenticationServiceException("invalid request");
        }

        // fetch first non null response
        ClientAuthentication auth = null;
        for (ClientAuthenticationProvider provider : providers) {
            if (!provider.supports(authentication.getClass())) {
                continue;
            }

            // we let authExceptions propagate
            auth = provider.authenticate(authentication);
            if (auth != null) {
                break;
            }
        }

        if (auth == null) {
            throw new ProviderNotFoundException("provider not found");
        }

        // fetch client details
        String clientId = auth.getClientId();
        try {
            ClientDetails clientDetails = clientService.loadClient(clientId);
            auth.setClient(clientDetails);

            return auth;
        } catch (NoSuchClientException e) {
            throw new BadCredentialsException("invalid authentication request");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(clientService, "client details service is required");
    }
}
