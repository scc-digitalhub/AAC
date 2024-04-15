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

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.Assert;

public class OAuth2AuthorizationRequestEvent extends OAuth2MessageEvent {

    public OAuth2AuthorizationRequestEvent(
        String authority,
        String provider,
        String realm,
        OAuth2AuthorizationRequest request
    ) {
        super(authority, provider, realm, request);
        Assert.notNull(request, "request can not be null");
    }

    public OAuth2AuthorizationRequest getAuthorizationRequest() {
        return (OAuth2AuthorizationRequest) super.getSource();
    }

    @Override
    public String getTx() {
        return getAuthorizationRequest().getState();
    }
}
