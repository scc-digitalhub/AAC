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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.audit.helper.AuditHelperMethods;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientAuthenticationToken;
import it.smartcommunitylab.aac.oauth.event.OAuth2AuthorizationExceptionEvent;
import it.smartcommunitylab.aac.oauth.event.OAuth2Event;
import it.smartcommunitylab.aac.oauth.event.OAuth2TokenExceptionEvent;
import it.smartcommunitylab.aac.oauth.event.TokenGrantEvent;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.realms.service.RealmService;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.Authentication;
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

    private RealmService realmService;

    public OAuth2EventListener(OAuth2ClientDetailsService clientService) {
        Assert.notNull(clientService, "client service is required");
        this.clientService = clientService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
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
        data.put("realm", realm);
        data.put("error", errorCode);
        data.put("summary", exception.getSummary());
        data.put("message", exception.getMessage());
        data.put("info", exception.getAdditionalInformation());

        // build audit
        AuditEvent audit = new AuditEvent(Instant.now(), principal, type, data);

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
        data.put("realm", realm);
        data.put("error", errorCode);
        data.put("summary", exception.getSummary());
        data.put("message", exception.getMessage());
        data.put("info", exception.getAdditionalInformation());

        // build audit
        AuditEvent audit = new AuditEvent(Instant.now(), principal, type, data);

        // publish as event, listener will persist to store
        publish(audit);
    }

    public void onTokenGrantEvent(TokenGrantEvent event) {
        logger.debug("token grant event " + event.toString());
        if (event.getToken() instanceof AACOAuth2AccessToken) {
            AACOAuth2AccessToken token = (AACOAuth2AccessToken) event.getToken();
            OAuth2Authentication auth = event.getAuthentication();
            OAuth2ClientAuthenticationToken authClient = event.getClientAuthentication();
            Authentication rawAuthentication = auth.getUserAuthentication();

            String principal = token.getSubject();
            if (!StringUtils.hasText(principal)) {
                principal = auth.getName();
            }

            String realm = token.getRealm();
            // realm level detail configuration
            String levelRealmEvent = AuditHelperMethods.resolveOauth2EventsLevel(realmService, realm);

            if(levelRealmEvent.equals(SystemKeys.EVENTS_LEVEL_NONE)) {
                return;
            }

            // use LinkedHashMap so the serialized audit JSON preserves this insertion order
            Map<String, Object> data = new LinkedHashMap<>();

            // TOKEN GRANT TYPE
            if (StringUtils.hasText(event.getGrantType())) {
                data.put("grant_type", event.getGrantType());
            }

            // ISSUED TOKENS
            List<String> issuedTokens = AuditHelperMethods.issuedTokens(token, event);
            data.put("issued_tokens", issuedTokens);

            // IP ADDRESS OF CLIENT THAT REQUIRE TOKEN
            if (!levelRealmEvent.equals(SystemKeys.EVENTS_LEVEL_MINIMAL) && authClient != null && authClient.getWebAuthenticationDetails() != null) {
                data.put("webAuthenticationDetails", authClient.getWebAuthenticationDetails());
            }

            // CLIENT DATA
            if (authClient != null) {
                Map<String, Object> clientData = AuditHelperMethods.clientData(authClient, realm);
                data.put("client", clientData);
            }

            // USER DATA
            if (rawAuthentication != null && rawAuthentication.getDetails() != null) {
                Map<String, Object> userData = AuditHelperMethods.userData(rawAuthentication.getDetails(), levelRealmEvent);
                data.put("user", userData);
            }

            // TOKEN TYPE
            String tokenType = AuditHelperMethods.tokenType(token, rawAuthentication);
            data.put("type", tokenType);

            // TOKEN VALUE SANITIZE
            if (!levelRealmEvent.equals(SystemKeys.EVENTS_LEVEL_MINIMAL)) {
                String safeTokenValue = AuditHelperMethods.sanitizeToken(token.getValue());
                data.put("token", safeTokenValue);
            }
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
            AuditEvent audit = new AuditEvent(Instant.now(), principal, TOKEN_GRANT, data);

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
