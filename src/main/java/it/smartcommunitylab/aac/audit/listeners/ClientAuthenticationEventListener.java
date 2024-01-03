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

package it.smartcommunitylab.aac.audit.listeners;

import it.smartcommunitylab.aac.core.auth.ClientAuthentication;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.security.AbstractAuthenticationAuditListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.AuthenticationException;

public class ClientAuthenticationEventListener extends AbstractAuthenticationAuditListener {

    public static final String CLIENT_AUTHENTICATION_FAILURE = "CLIENT_AUTHENTICATION_FAILURE";
    public static final String CLIENT_AUTHENTICATION_SUCCESS = "CLIENT_AUTHENTICATION_SUCCESS";

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        if (event instanceof AuthenticationSuccessEvent) {
            onAuthenticationSuccessEvent((AuthenticationSuccessEvent) event);
        } else if (event instanceof AbstractAuthenticationFailureEvent) {
            onAuthenticationFailureEvent((AbstractAuthenticationFailureEvent) event);
        }
    }

    private void onAuthenticationFailureEvent(AbstractAuthenticationFailureEvent event) {
        AuthenticationException ex = event.getException();
        if (!(event.getAuthentication() instanceof ClientAuthentication)) {
            return;
        }

        ClientAuthentication auth = (ClientAuthentication) event.getAuthentication();
        String principal = auth.getPrincipal();
        Object details = auth.getDetails();
        String eventType = CLIENT_AUTHENTICATION_FAILURE;

        // build data
        Map<String, Object> data = new HashMap<>();
        data.put("type", ex.getClass().getName());
        data.put("message", ex.getMessage());
        data.put("auth", auth.getClass().getName());

        // persist details, should be safe to store
        if (details != null) {
            data.put("details", details);
        }

        // build audit
        AuditEvent audit = new AuditEvent(principal, eventType, data);

        // publish as event, listener will persist to store
        publish(audit);
    }

    private void onAuthenticationSuccessEvent(AuthenticationSuccessEvent event) {
        if (!(event.getAuthentication() instanceof ClientAuthentication)) {
            return;
        }

        ClientAuthentication auth = (ClientAuthentication) event.getAuthentication();
        String principal = auth.getName();
        Object details = auth.getDetails();
        String eventType = CLIENT_AUTHENTICATION_SUCCESS;

        Map<String, Object> data = new HashMap<>();
        data.put("auth", auth.getClass().getName());
        data.put("realm", auth.getRealm());

        // persist details, should be safe to store
        if (details != null) {
            data.put("details", details);
        }

        // build audit
        AuditEvent audit = new AuditEvent(principal, eventType, data);

        // publish as event, listener will persist to store
        publish(audit);
    }
}
