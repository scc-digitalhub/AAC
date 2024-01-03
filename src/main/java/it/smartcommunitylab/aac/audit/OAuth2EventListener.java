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

package it.smartcommunitylab.aac.audit;

import it.smartcommunitylab.aac.audit.model.RealmAuditEvent;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.event.OAuth2AuthorizationExceptionEvent;
import it.smartcommunitylab.aac.oauth.event.OAuth2Event;
import it.smartcommunitylab.aac.oauth.event.OAuth2TokenExceptionEvent;
import it.smartcommunitylab.aac.oauth.event.TokenGrantEvent;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OAuth2EventListener implements ApplicationListener<OAuth2Event>, ApplicationEventPublisherAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String TOKEN_GRANT = "OAUTH2_TOKEN_GRANT";

    private ApplicationEventPublisher publisher;

    private final OAuth2ClientDetailsService clientService;

    public OAuth2EventListener(OAuth2ClientDetailsService clientService) {
        Assert.notNull(clientService, "client service is required");
        this.clientService = clientService;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    protected ApplicationEventPublisher getPublisher() {
        return this.publisher;
    }

    public void onApplicationEvent(OAuth2Event event) {
        try {
            if (event instanceof TokenGrantEvent) {
                onTokenGrantEvent((TokenGrantEvent) event);
            } else if (event instanceof OAuth2AuthorizationExceptionEvent) {
                onAuthorizationExceptionEvent((OAuth2AuthorizationExceptionEvent) event);
            } else if (event instanceof OAuth2TokenExceptionEvent) {
                onTokenExceptionEvent((OAuth2TokenExceptionEvent) event);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private void onAuthorizationExceptionEvent(OAuth2AuthorizationExceptionEvent event) {
        logger.debug("exception event " + event.toString());

        OAuth2Exception exception = event.getException();
        AuthorizationRequest request = event.getAuthorizationRequest();
        OAuth2Authentication auth = event.getAuthentication();

        String clientId = request.getClientId();
        OAuth2ClientDetails clientDetails = clientService.loadClientByClientId(clientId);

        String principal = auth != null ? auth.getName() : clientId;
        String realm = clientDetails.getRealm();

        String errorCode = exception.getOAuth2ErrorCode();
        String type = "OAUTH2_" + errorCode.toUpperCase();

        Map<String, Object> data = new HashMap<>();
        data.put("error", errorCode);
        data.put("summary", exception.getSummary());
        data.put("message", exception.getMessage());
        data.put("info", exception.getAdditionalInformation());

        // build audit
        RealmAuditEvent audit = new RealmAuditEvent(realm, Instant.now(), principal, type, data);

        // publish as event, listener will persist to store
        publish(audit);
    }

    private void onTokenExceptionEvent(OAuth2TokenExceptionEvent event) {
        logger.debug("exception event " + event.toString());

        OAuth2Exception exception = event.getException();
        TokenRequest request = event.getTokenRequest();
        OAuth2Authentication auth = event.getAuthentication();

        String clientId = request.getClientId();
        OAuth2ClientDetails clientDetails = clientService.loadClientByClientId(clientId);

        String principal = auth != null ? auth.getName() : clientId;
        String realm = clientDetails.getRealm();

        String errorCode = exception.getOAuth2ErrorCode();
        String type = "OAUTH2_" + errorCode.toUpperCase();

        Map<String, Object> data = new HashMap<>();
        data.put("error", errorCode);
        data.put("summary", exception.getSummary());
        data.put("message", exception.getMessage());
        data.put("info", exception.getAdditionalInformation());

        // build audit
        RealmAuditEvent audit = new RealmAuditEvent(realm, Instant.now(), principal, type, data);

        // publish as event, listener will persist to store
        publish(audit);
    }

    public void onTokenGrantEvent(TokenGrantEvent event) {
        logger.debug("token grant event " + event.toString());
        if (event.getToken() instanceof AACOAuth2AccessToken) {
            AACOAuth2AccessToken token = (AACOAuth2AccessToken) event.getToken();
            OAuth2Authentication auth = event.getAuthentication();

            //            String principal = auth.getName();
            String principal = token.getSubject();
            if (!StringUtils.hasText(principal)) {
                principal = auth.getName();
            }

            String realm = token.getRealm();
            String type = auth.getUserAuthentication() == null ? "client" : "user";

            Map<String, Object> data = new HashMap<>();
            data.put("type", type);
            data.put("token", token.getValue());
            data.put("scope", token.getScope());

            data.put("jti", token.getToken());
            data.put("realm", token.getRealm());
            data.put("expiration", token.getExpiration());
            data.put("issuedAt", token.getIssuedAt());

            if (token.getAudience() != null) {
                data.put("audience", StringUtils.collectionToCommaDelimitedString(Arrays.asList(token.getAudience())));
            }

            if (token.getAuthorizedParty() != null) {
                data.put("authorizedParty", token.getAuthorizedParty());
            }

            // build audit
            RealmAuditEvent audit = new RealmAuditEvent(realm, Instant.now(), principal, TOKEN_GRANT, data);

            // publish as event, listener will persist to store
            publish(audit);
        }
    }

    protected void publish(AuditEvent event) {
        if (getPublisher() != null) {
            getPublisher().publishEvent(new AuditApplicationEvent(event));
        }
    }
}
