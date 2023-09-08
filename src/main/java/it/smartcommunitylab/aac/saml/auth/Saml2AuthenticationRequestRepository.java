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

package it.smartcommunitylab.aac.saml.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestContext;

public interface Saml2AuthenticationRequestRepository<T extends SerializableSaml2AuthenticationRequestContext> {
    T loadAuthenticationRequest(HttpServletRequest request);

    void saveAuthenticationRequest(T authenticationRequest, HttpServletRequest request, HttpServletResponse response);

    T removeAuthenticationRequest(HttpServletRequest request, HttpServletResponse response);
}
