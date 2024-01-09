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

import it.smartcommunitylab.aac.oidc.events.OAuth2UserRequestEvent;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequestEntityConverter;
import org.springframework.util.Assert;

public class OpenIdFedUserRequestEntityConverter
    extends OAuth2UserRequestEntityConverter
    implements ApplicationEventPublisherAware {

    private final OpenIdFedIdentityProviderConfig config;

    private ApplicationEventPublisher eventPublisher;

    public OpenIdFedUserRequestEntityConverter(OpenIdFedIdentityProviderConfig config) {
        Assert.notNull(config, "provider config is required");
        this.config = config;
    }

    @Override
    public RequestEntity<?> convert(OAuth2UserRequest userRequest) {
        if (eventPublisher != null) {
            OAuth2UserRequestEvent event = new OAuth2UserRequestEvent(
                config.getAuthority(),
                config.getProvider(),
                config.getRealm(),
                userRequest
            );

            eventPublisher.publishEvent(event);
        }

        return super.convert(userRequest);
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
}
