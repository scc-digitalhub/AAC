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

package it.smartcommunitylab.aac.clients.auth;

import it.smartcommunitylab.aac.clients.service.ClientDetailsService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public abstract class ClientAuthenticationProvider implements AuthenticationProvider {

    protected ClientDetailsService clientService;

    @Override
    public abstract ClientAuthentication authenticate(Authentication authentication) throws AuthenticationException;

    public void setClientService(ClientDetailsService clientService) {
        this.clientService = clientService;
    }
}
