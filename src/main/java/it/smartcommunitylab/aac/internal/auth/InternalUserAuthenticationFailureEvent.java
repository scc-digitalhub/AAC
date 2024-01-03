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

package it.smartcommunitylab.aac.internal.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.audit.events.UserAuthenticationFailureEvent;

import java.io.Serializable;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class InternalUserAuthenticationFailureEvent extends UserAuthenticationFailureEvent {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    // TODO evaluate adding fields for error and message

    public InternalUserAuthenticationFailureEvent(
        String authority,
        String provider,
        String realm,
        Authentication authentication,
        InternalAuthenticationException exception
    ) {
        super(authority, provider, realm, exception.getSubject(), authentication, exception);
    }

    @Override
    public Map<String, Serializable> exportException() {
        Map<String, Serializable> data = super.exportException();

        InternalAuthenticationException iex = (InternalAuthenticationException) getException();
        AuthenticationException ex = iex.getException();

        data.put("error", iex.getMessage());
        data.put("errorCode", ex.getClass().getSimpleName());
        data.put("flow", iex.getFlow());

        return data;
    }

    @Override
    public Map<String, Serializable> exportAuthentication() {
        Map<String, Serializable> data = super.exportAuthentication();
        Authentication auth = getAuthentication();
        InternalAuthenticationException iex = (InternalAuthenticationException) getException();

        String username = auth.getName();
        String credentials = String.valueOf(auth.getCredentials());

        data.put("username", username);
        data.put("credentials", credentials);
        data.put("flow", iex.getFlow());

        return data;
    }
}
