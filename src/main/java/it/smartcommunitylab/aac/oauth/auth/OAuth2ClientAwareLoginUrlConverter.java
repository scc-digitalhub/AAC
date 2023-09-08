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

import it.smartcommunitylab.aac.core.auth.LoginUrlRequestConverter;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OAuth2ClientAwareLoginUrlConverter implements LoginUrlRequestConverter {

    public static final String CLIENT_ID_PARAMETER_NAME = "client_id";

    private final OAuth2ClientDetailsService clientDetailsService;
    private String loginUrl;

    public OAuth2ClientAwareLoginUrlConverter(OAuth2ClientDetailsService clientDetailsService, String loginFormUrl) {
        Assert.notNull(clientDetailsService, "clientDetails service is required");
        this.clientDetailsService = clientDetailsService;
        this.loginUrl = loginFormUrl;
    }

    @Override
    public String convert(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) {
        // check if clientId via param
        String clientId = null;
        if (request.getParameter(CLIENT_ID_PARAMETER_NAME) != null) {
            clientId = request.getParameter(CLIENT_ID_PARAMETER_NAME);
        }

        // check if clientId via attribute
        if (request.getAttribute(CLIENT_ID_PARAMETER_NAME) != null) {
            clientId = (String) request.getAttribute(CLIENT_ID_PARAMETER_NAME);
        }
        // check if clientId
        if (StringUtils.hasText(clientId)) {
            try {
                OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);
                String realm = clientDetails.getRealm();

                return "/-/" + realm + loginUrl + "?" + CLIENT_ID_PARAMETER_NAME + "=" + clientId;
            } catch (ClientRegistrationException e) {
                return null;
            }
        }

        // not found
        return null;
    }
}
