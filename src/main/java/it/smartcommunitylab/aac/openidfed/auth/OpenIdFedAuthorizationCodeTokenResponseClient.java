/**
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.openidfed.auth;

import it.smartcommunitylab.aac.oidc.events.OAuth2TokenResponseEvent;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.util.Assert;

public class OpenIdFedAuthorizationCodeTokenResponseClient
    implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>, ApplicationEventPublisherAware {

    private final OpenIdFedIdentityProviderConfig config;

    private final OpenIdFedAuthorizationCodeRequestEntityConverter requestEntityConverter;
    private final DefaultAuthorizationCodeTokenResponseClient accessTokenResponseClient;

    private ApplicationEventPublisher eventPublisher;

    public OpenIdFedAuthorizationCodeTokenResponseClient(OpenIdFedIdentityProviderConfig config) {
        Assert.notNull(config, "provider config is required");
        this.config = config;

        requestEntityConverter = new OpenIdFedAuthorizationCodeRequestEntityConverter(config);
        requestEntityConverter.setApplicationEventPublisher(eventPublisher);

        accessTokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
        accessTokenResponseClient.setRequestEntityConverter(requestEntityConverter);
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.requestEntityConverter.setApplicationEventPublisher(eventPublisher);
    }

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest authorizationGrantRequest) {
        OAuth2AccessTokenResponse response = accessTokenResponseClient.getTokenResponse(authorizationGrantRequest);

        if (eventPublisher != null) {
            OAuth2TokenResponseEvent event = new OAuth2TokenResponseEvent(
                config.getAuthority(),
                config.getProvider(),
                config.getRealm(),
                response
            );
            event.setTx(authorizationGrantRequest.getAuthorizationExchange().getAuthorizationRequest().getState());

            eventPublisher.publishEvent(event);
        }

        return response;
    }
}
