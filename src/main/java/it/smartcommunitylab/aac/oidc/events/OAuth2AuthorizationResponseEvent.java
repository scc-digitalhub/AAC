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

package it.smartcommunitylab.aac.oidc.events;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.util.Assert;

public class OAuth2AuthorizationResponseEvent extends OAuth2MessageEvent {

    public OAuth2AuthorizationResponseEvent(
        String authority,
        String provider,
        String realm,
        OAuth2AuthorizationResponse response
    ) {
        super(authority, provider, realm, response);
        Assert.notNull(response, "response can not be null");
    }

    public OAuth2AuthorizationResponse getAuthorizationResponse() {
        return (OAuth2AuthorizationResponse) super.getSource();
    }

    @Override
    public String getTx() {
        return getAuthorizationResponse().getState();
    }
}
