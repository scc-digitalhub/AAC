package it.smartcommunitylab.aac.audit;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.security.AbstractAuthenticationAuditListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ClientAuthentication;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.auth.WrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.persistence.ProviderEntity;
import it.smartcommunitylab.aac.core.service.ProviderService;

public class AuthenticationEventListener extends AbstractAuthenticationAuditListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String AUTHENTICATION_SUCCESS = "AUTHENTICATION_SUCCESS";
    public static final String AUTHENTICATION_FAILURE = "AUTHENTICATION_FAILURE";

    public static final String USER_AUTHENTICATION_FAILURE = "USER_AUTHENTICATION_FAILURE";
    public static final String USER_AUTHENTICATION_SUCCESS = "USER_AUTHENTICATION_SUCCESS";

    public static final String CLIENT_AUTHENTICATION_FAILURE = "CLIENT_AUTHENTICATION_FAILURE";
    public static final String CLIENT_AUTHENTICATION_SUCCESS = "CLIENT_AUTHENTICATION_SUCCESS";

    private ProviderService providerService;

    public void setProviderService(ProviderService providerService) {
        this.providerService = providerService;
    }

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        if (event instanceof UserAuthenticationFailureEvent) {
            onUserAuthenticationFailureEvent((UserAuthenticationFailureEvent) event);
        } else if (event instanceof UserAuthenticationSuccessEvent) {
            onUserAuthenticationSuccessEvent((UserAuthenticationSuccessEvent) event);
        } else if (event instanceof AuthenticationSuccessEvent) {
            onAuthenticationSuccessEvent((AuthenticationSuccessEvent) event);
        } else if (event instanceof AbstractAuthenticationFailureEvent) {
            onAuthenticationFailureEvent((AbstractAuthenticationFailureEvent) event);
        }
    }

    private void onUserAuthenticationFailureEvent(UserAuthenticationFailureEvent event) {
        AuthenticationException ex = event.getException();
        Authentication authentication = event.getAuthentication();
        String principal = authentication.getName();

        String authority = event.getAuthority();
        String provider = event.getProvider();
        String realm = event.getRealm();

        String level = SystemKeys.EVENTS_LEVEL_DETAILS;
        String eventType = USER_AUTHENTICATION_FAILURE;

        if (providerService != null) {
            ProviderEntity p = providerService.findProvider(provider);
            if (p != null && p.getEvents() != null) {
                level = p.getEvents();
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("authority", authority);
        data.put("provider", provider);
        data.put("realm", realm);
        data.put("type", ex.getClass().getSimpleName());
        data.put("message", ex.getMessage());

        if (SystemKeys.EVENTS_LEVEL_DETAILS.equals(level) || SystemKeys.EVENTS_LEVEL_FULL.equals(level)) {
            if (ex instanceof Serializable) {
                data.put("exception", event.exportException());
            }

            if (authentication instanceof WrappedAuthenticationToken) {
                // persist web details, should be safe to store
                data.put("details", ((WrappedAuthenticationToken) authentication).getAuthenticationDetails());
            }
        }

        if (SystemKeys.EVENTS_LEVEL_FULL.equals(level)) {
            // persist full authentication token
            // TODO add export
            data.put("authentication", authentication);
        }

        // build audit
        RealmAuditEvent audit = new RealmAuditEvent(realm, Instant.now(), principal, eventType, data);

        // publish as event, listener will persist to store
        publish(audit);
    }

    private void onUserAuthenticationSuccessEvent(UserAuthenticationSuccessEvent event) {
        UserAuthentication auth = event.getAuthenticationToken();
        String principal = auth.getSubjectId();
        String authority = event.getAuthority();
        String provider = event.getProvider();
        String realm = event.getRealm();

        String level = SystemKeys.EVENTS_LEVEL_DETAILS;
        String eventType = USER_AUTHENTICATION_SUCCESS;

        if (providerService != null) {
            ProviderEntity p = providerService.findProvider(provider);
            if (p != null && p.getEvents() != null) {
                level = p.getEvents();
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("authority", authority);
        data.put("provider", provider);
        data.put("realm", realm);

        if (SystemKeys.EVENTS_LEVEL_DETAILS.equals(level) || SystemKeys.EVENTS_LEVEL_FULL.equals(level)) {

            // persist web details, should be safe to store
            if (auth.getWebAuthenticationDetails() != null) {
                data.put("details", auth.getWebAuthenticationDetails());
            }

        }

        if (SystemKeys.EVENTS_LEVEL_FULL.equals(level)) {
            // persist full authentication token
            // TODO add export
            data.put("authentication", auth);
        }

        // build audit
        RealmAuditEvent audit = new RealmAuditEvent(realm, Instant.now(), principal, eventType, data);

        // publish as event, listener will persist to store
        publish(audit);
    }

    private void onAuthenticationFailureEvent(AbstractAuthenticationFailureEvent event) {

        AuthenticationException ex = event.getException();
        Authentication auth = event.getAuthentication();
        String principal = "";
        Object details = auth.getDetails();
        String eventType = AUTHENTICATION_FAILURE;

        // try to extract details if wrapped
        if (auth instanceof WrappedAuthenticationToken) {
            WrappedAuthenticationToken token = (WrappedAuthenticationToken) auth;
            eventType = USER_AUTHENTICATION_FAILURE;
            auth = token.getAuthenticationToken();
            principal = auth.getName();
            details = token.getAuthenticationDetails();

        }

        if (auth instanceof ClientAuthentication) {
            ClientAuthentication token = (ClientAuthentication) auth;
            eventType = CLIENT_AUTHENTICATION_FAILURE;
            details = token.getWebAuthenticationDetails();
        }

        if (auth instanceof BearerTokenAuthenticationToken) {
            // principal is token, we ignore for now
            // we could store in data to support reuse detection etc
            // but JWT are large (>4k) and expensive
            principal = "";
        }

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

        Authentication auth = event.getAuthentication();
        String principal = auth.getName();
        Object details = auth.getDetails();
        String eventType = AUTHENTICATION_SUCCESS;

        Map<String, Object> data = new HashMap<>();
        data.put("auth", auth.getClass().getName());

        // persist details, should be safe to store
        if (details != null) {
            data.put("details", details);
        }

        // check if user auth
        if (auth instanceof UserAuthentication) {
            UserAuthentication token = (UserAuthentication) auth;
            eventType = USER_AUTHENTICATION_SUCCESS;
            data.put("realm", token.getRealm());
            // TODO get last provider from tokens, needs ordering or dedicated field
        }
        if (auth instanceof ClientAuthentication) {
            ClientAuthentication token = (ClientAuthentication) auth;
            eventType = CLIENT_AUTHENTICATION_SUCCESS;
            details = token.getWebAuthenticationDetails();
            data.put("realm", token.getRealm());

        }

        // build audit
        AuditEvent audit = new AuditEvent(principal, eventType, data);

        // publish as event, listener will persist to store
        publish(audit);
    }
}
