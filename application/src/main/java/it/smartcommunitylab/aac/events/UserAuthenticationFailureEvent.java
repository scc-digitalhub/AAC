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

package it.smartcommunitylab.aac.events;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.core.auth.WrappedAuthenticationToken;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;

//TODO add custom serializer
//TODO add subtype inference

public class UserAuthenticationFailureEvent extends AbstractAuthenticationFailureEvent {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String authority;
    private final String provider;
    private final String realm;
    private final String subject;

    public UserAuthenticationFailureEvent(
        String authority,
        String provider,
        String realm,
        String subject,
        Authentication authentication,
        AuthenticationException exception
    ) {
        super(authentication, exception);
        Assert.hasText(authority, "authority is required");
        Assert.notNull(realm, "realm is required");

        this.authority = authority;
        this.provider = provider;
        this.realm = realm;
        this.subject = subject != null ? subject : authentication.getName();
    }

    public String getAuthority() {
        return authority;
    }

    public String getProvider() {
        return provider;
    }

    public String getRealm() {
        return realm;
    }

    public String getSubject() {
        return subject;
    }

    public Map<String, Serializable> exportException() {
        Map<String, Serializable> data = new HashMap<>();
        AuthenticationException ex = getException();
        data.put("type", ex.getClass().getName());
        data.put("message", ex.getMessage());

        return data;
    }

    public Map<String, Serializable> exportAuthentication() {
        Map<String, Serializable> data = new HashMap<>();
        Authentication auth = getAuthentication();
        if (auth instanceof WrappedAuthenticationToken) {
            auth = ((WrappedAuthenticationToken) getAuthentication()).getAuthenticationToken();
        }

        data.put("type", auth.getClass().getName());
        data.put("principal", subject);

        return data;
    }

    public WebAuthenticationDetails getAuthenticationDetails() {
        Authentication auth = getAuthentication();
        if (auth instanceof WrappedAuthenticationToken) {
            return ((WrappedAuthenticationToken) auth).getAuthenticationDetails();
        }

        return null;
    }
}
