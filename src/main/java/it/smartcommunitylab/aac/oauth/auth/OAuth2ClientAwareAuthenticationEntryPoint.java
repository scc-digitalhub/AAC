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

import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OAuth2ClientAwareAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    public static final String CLIENT_ID_PARAMETER_NAME = "client_id";

    private final OAuth2ClientDetailsService clientDetailsService;

    public OAuth2ClientAwareAuthenticationEntryPoint(
        OAuth2ClientDetailsService clientDetailsService,
        String loginFormUrl
    ) {
        super(loginFormUrl);
        Assert.notNull(clientDetailsService, "client details service is required");
        this.clientDetailsService = clientDetailsService;
    }

    @Override
    protected String determineUrlToUseForThisRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) {
        // check if clientId
        if (StringUtils.hasText(request.getParameter(CLIENT_ID_PARAMETER_NAME))) {
            String clientId = request.getParameter(CLIENT_ID_PARAMETER_NAME);
            try {
                OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);
                String realm = clientDetails.getRealm();

                return "/-/" + realm + getLoginFormUrl() + "?" + CLIENT_ID_PARAMETER_NAME + "=" + clientId;
            } catch (ClientRegistrationException e) {
                // send to error page
                return "/error";
            }
        }

        // return global
        return getLoginFormUrl();
    }
}
