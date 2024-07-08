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
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.StringUtils;

public class ClientRefreshAuthenticationConverter extends OAuth2ClientAuthenticationConverter {

    @Override
    public OAuth2ClientRefreshAuthenticationToken attemptConvert(HttpServletRequest request) {
        // we support only POST requests
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        // fetch and validate parameters
        // if client provides a secret we'll skip and prioritize form auth
        Map<String, String[]> parameters = request.getParameterMap();
        if (
            !parameters.containsKey(OAuth2ParameterNames.CLIENT_ID) ||
            !parameters.containsKey(OAuth2ParameterNames.REFRESH_TOKEN) ||
            !parameters.containsKey(OAuth2ParameterNames.GRANT_TYPE) ||
            parameters.containsKey(OAuth2ParameterNames.CLIENT_SECRET)
        ) {
            // not a valid request
            return null;
        }

       // if client provides a auth header we'll skip and prioritize basic auth      
       if (
           request.getHeader(HttpHeaders.AUTHORIZATION) != null
       ) {
           // not a valid request
           return null;
       }

        // support refresh flow without secret for public clients
        // requires refresh token rotation set
        AuthorizationGrantType grantType = AuthorizationGrantType.parse(
            request.getParameter(OAuth2ParameterNames.GRANT_TYPE)
        );

        if (AuthorizationGrantType.REFRESH_TOKEN != grantType) {
            return null;
        }

        // make sure we get exactly 1 value per parameter
        if (
            parameters.get(OAuth2ParameterNames.CLIENT_ID).length != 1 ||
            parameters.get(OAuth2ParameterNames.REFRESH_TOKEN).length != 1
        ) {
            // throw oauth2 exception
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        // read first (only) value from parameters
        String clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID);
        String refreshToken = request.getParameter(OAuth2ParameterNames.REFRESH_TOKEN);

        // validate parameters are *not* empty
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(refreshToken)) {
            // throw oauth2 exception
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST));
        }

        // return our authRequest
        return new OAuth2ClientRefreshAuthenticationToken(clientId, refreshToken, AuthenticationMethod.NONE.getValue());
    }
}
