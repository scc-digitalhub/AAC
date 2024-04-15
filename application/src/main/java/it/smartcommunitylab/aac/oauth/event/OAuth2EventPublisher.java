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

package it.smartcommunitylab.aac.oauth.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.TokenRequest;

public class OAuth2EventPublisher implements ApplicationEventPublisherAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ApplicationEventPublisher applicationEventPublisher;

    public OAuth2EventPublisher() {
        this(null);
    }

    public OAuth2EventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishTokenGrant(OAuth2AccessToken token, OAuth2Authentication authentication) {
        if (this.applicationEventPublisher != null) {
            this.applicationEventPublisher.publishEvent(new TokenGrantEvent(token, authentication));
        }
    }

    public void publishOAuth2AuthorizationException(
        AuthorizationRequest request,
        OAuth2Exception exception,
        OAuth2Authentication authentication
    ) {
        if (this.applicationEventPublisher != null) {
            this.applicationEventPublisher.publishEvent(
                    new OAuth2AuthorizationExceptionEvent(request, exception, authentication)
                );
        }
    }

    public void publishOAuth2TokenException(
        TokenRequest request,
        OAuth2Exception exception,
        OAuth2Authentication authentication
    ) {
        if (this.applicationEventPublisher != null) {
            this.applicationEventPublisher.publishEvent(
                    new OAuth2TokenExceptionEvent(request, exception, authentication)
                );
        }
    }
}
