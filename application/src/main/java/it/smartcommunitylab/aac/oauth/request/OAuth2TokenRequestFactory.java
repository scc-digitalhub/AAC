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

package it.smartcommunitylab.aac.oauth.request;

import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import java.util.Map;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.OAuth2Request;

public interface OAuth2TokenRequestFactory {
    TokenRequest createTokenRequest(Map<String, String> requestParameters, OAuth2ClientDetails clientDetails);

    TokenRequest createTokenRequest(AuthorizationRequest authorizationRequest, String grantType);

    OAuth2Request createOAuth2Request(TokenRequest tokenRequest, OAuth2ClientDetails client);
}
