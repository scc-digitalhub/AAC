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

package it.smartcommunitylab.aac.oauth.auth;

import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.StringUtils;

public class ClientSecretPostAuthenticationConverter extends OAuth2ClientAuthenticationConverter {

    @Override
    public OAuth2ClientSecretAuthenticationToken attemptConvert(HttpServletRequest request) {
        // we support only POST requests
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        // fetch and validate parameters
        Map<String, String[]> parameters = request.getParameterMap();
        if (
            !parameters.containsKey(OAuth2ParameterNames.CLIENT_ID) ||
            !parameters.containsKey(OAuth2ParameterNames.CLIENT_SECRET)
        ) {
            // not a valid request
            return null;
        }

        // make sure we get only 1 clientId and 1 secret in post
        if (
            parameters.get(OAuth2ParameterNames.CLIENT_ID).length != 1 ||
            parameters.get(OAuth2ParameterNames.CLIENT_SECRET).length != 1
        ) {
            // throw oauth2 exception
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        // read first (only) value from parameters
        String clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID);
        String clientSecret = request.getParameter(OAuth2ParameterNames.CLIENT_SECRET);

        // validate both clientId and secret are *not* empty
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            // throw oauth2 exception
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST));
        }

        // return our authRequest
        return new OAuth2ClientSecretAuthenticationToken(
            clientId,
            clientSecret,
            AuthenticationMethod.CLIENT_SECRET_POST.getValue()
        );
    }
}
