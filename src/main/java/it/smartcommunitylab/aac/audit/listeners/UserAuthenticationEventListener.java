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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.auth.WrappedAuthenticationToken;
import it.smartcommunitylab.aac.events.UserAuthenticationFailureEvent;
import it.smartcommunitylab.aac.events.UserAuthenticationSuccessEvent;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.service.IdentityProviderService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.security.AbstractAuthenticationAuditListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;

public class UserAuthenticationEventListener extends AbstractAuthenticationAuditListener {

    // public static final String AUTHENTICATION_SUCCESS = "AUTHENTICATION_SUCCESS";
    // public static final String AUTHENTICATION_FAILURE = "AUTHENTICATION_FAILURE";

    public static final String USER_AUTHENTICATION_FAILURE = "USER_AUTHENTICATION_FAILURE";
    public static final String USER_AUTHENTICATION_SUCCESS = "USER_AUTHENTICATION_SUCCESS";

    //TODO replace with identity authority provider service to read only *active* providers
    private IdentityProviderService providerService;

    public void setProviderService(IdentityProviderService providerService) {
        this.providerService = providerService;
    }

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        if (event instanceof UserAuthenticationFailureEvent) {
            onUserAuthenticationFailureEvent((UserAuthenticationFailureEvent) event);
        } else if (event instanceof UserAuthenticationSuccessEvent) {
            onUserAuthenticationSuccessEvent((UserAuthenticationSuccessEvent) event);
            // } else if (event instanceof AuthenticationSuccessEvent) {
            //     onAuthenticationSuccessEvent((AuthenticationSuccessEvent) event);
            // } else if (event instanceof AbstractAuthenticationFailureEvent) {
            //     onAuthenticationFailureEvent((AbstractAuthenticationFailureEvent) event);
        }
    }

    private void onUserAuthenticationFailureEvent(UserAuthenticationFailureEvent event) {
        AuthenticationException ex = event.getException();
        Authentication authentication = event.getAuthentication();
        String principal = event.getSubject();

        String authority = event.getAuthority();
        String provider = event.getProvider();
        String realm = event.getRealm();

        String level = SystemKeys.EVENTS_LEVEL_DETAILS;
        String eventType = USER_AUTHENTICATION_FAILURE;

        if (providerService != null) {
            ConfigurableIdentityProvider p = providerService.findProvider(provider);
            if (
                p != null &&
                p.getSettings() != null &&
                StringUtils.hasText(String.valueOf(p.getSettings().get("events")))
            ) {
                level = String.valueOf(p.getSettings().get("events"));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("authority", authority);
        data.put("provider", provider);
        data.put("realm", realm);
        data.put("message", ex.getMessage());

        if (authentication instanceof WrappedAuthenticationToken) {
            // persist web details, should be safe to store
            data.put("details", ((WrappedAuthenticationToken) authentication).getAuthenticationDetails());
        }

        if (SystemKeys.EVENTS_LEVEL_DETAILS.equals(level) || SystemKeys.EVENTS_LEVEL_FULL.equals(level)) {
            data.put("exception", event.exportException());
        }

        if (SystemKeys.EVENTS_LEVEL_FULL.equals(level)) {
            // persist full authentication token
            // export to ensure we can serialize the token
            data.put("authentication", event.exportAuthentication());
            //TODO store full event
        }

        // build audit
        AuditEvent audit = new AuditEvent(Instant.now(), principal, eventType, data);

        // publish as event, listener will persist to store
        publish(audit);
    }

    private void onUserAuthenticationSuccessEvent(UserAuthenticationSuccessEvent event) {
        UserAuthentication auth = event.getUserAuthentication();
        String principal = auth.getSubjectId();
        String authority = event.getAuthority();
        String provider = event.getProvider();
        String realm = event.getRealm();

        String level = SystemKeys.EVENTS_LEVEL_DETAILS;
        String eventType = USER_AUTHENTICATION_SUCCESS;

        if (providerService != null) {
            ConfigurableIdentityProvider p = providerService.findProvider(provider);
            if (
                p != null &&
                p.getSettings() != null &&
                StringUtils.hasText(String.valueOf(p.getSettings().get("events")))
            ) {
                level = String.valueOf(p.getSettings().get("events"));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("authority", authority);
        data.put("provider", provider);
        data.put("realm", realm);

        // persist web details, should be safe to store
        if (auth.getWebAuthenticationDetails() != null) {
            data.put("details", auth.getWebAuthenticationDetails());
        }

        if (SystemKeys.EVENTS_LEVEL_FULL.equals(level)) {
            // persist full authentication token
            // TODO add export
            // make sure credentials are cleared from this context
            auth.eraseCredentials();
            data.put("authentication", auth);
            //TODO store full event

        }

        // build audit
        AuditEvent audit = new AuditEvent(Instant.now(), principal, eventType, data);

        // publish as event, listener will persist to store
        publish(audit);
    }
    // private void onAuthenticationFailureEvent(AbstractAuthenticationFailureEvent event) {
    //     AuthenticationException ex = event.getException();
    //     Authentication auth = event.getAuthentication();
    //     String principal = "";
    //     Object details = auth.getDetails();
    //     String eventType = AUTHENTICATION_FAILURE;

    //     // try to extract details if wrapped
    //     if (auth instanceof WrappedAuthenticationToken) {
    //         WrappedAuthenticationToken token = (WrappedAuthenticationToken) auth;
    //         eventType = USER_AUTHENTICATION_FAILURE;
    //         auth = token.getAuthenticationToken();
    //         principal = auth.getName();
    //         details = token.getAuthenticationDetails();
    //     }

    //     if (auth instanceof ClientAuthentication) {
    //         ClientAuthentication token = (ClientAuthentication) auth;
    //         eventType = CLIENT_AUTHENTICATION_FAILURE;
    //         details = token.getWebAuthenticationDetails();
    //     }

    //     if (auth instanceof BearerTokenAuthenticationToken) {
    //         // principal is token, we ignore for now
    //         // we could store in data to support reuse detection etc
    //         // but JWT are large (>4k) and expensive
    //         principal = "";
    //     }

    //     // build data
    //     Map<String, Object> data = new HashMap<>();
    //     data.put("type", ex.getClass().getName());
    //     data.put("message", ex.getMessage());
    //     data.put("auth", auth.getClass().getName());

    //     // persist details, should be safe to store
    //     if (details != null) {
    //         data.put("details", details);
    //     }

    //     // build audit
    //     AuditEvent audit = new AuditEvent(principal, eventType, data);

    //     // publish as event, listener will persist to store
    //     publish(audit);
    // }

    // private void onAuthenticationSuccessEvent(AuthenticationSuccessEvent event) {
    //     Authentication auth = event.getAuthentication();
    //     String principal = auth.getName();
    //     Object details = auth.getDetails();
    //     String eventType = AUTHENTICATION_SUCCESS;

    //     Map<String, Object> data = new HashMap<>();
    //     data.put("auth", auth.getClass().getName());

    //     // persist details, should be safe to store
    //     if (details != null) {
    //         data.put("details", details);
    //     }

    //     // check if user auth
    //     if (auth instanceof UserAuthentication) {
    //         UserAuthentication token = (UserAuthentication) auth;
    //         eventType = USER_AUTHENTICATION_SUCCESS;
    //         data.put("realm", token.getRealm());
    //         // TODO get last provider from tokens, needs ordering or dedicated field
    //     }
    //     if (auth instanceof ClientAuthentication) {
    //         ClientAuthentication token = (ClientAuthentication) auth;
    //         eventType = CLIENT_AUTHENTICATION_SUCCESS;
    //         details = token.getWebAuthenticationDetails();
    //         data.put("realm", token.getRealm());
    //     }

    //     // build audit
    //     AuditEvent audit = new AuditEvent(principal, eventType, data);

    //     // publish as event, listener will persist to store
    //     publish(audit);
    // }
}
