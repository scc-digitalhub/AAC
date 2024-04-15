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
import it.smartcommunitylab.aac.oauth.request.OAuth2AuthorizationRequestFactory;
import it.smartcommunitylab.aac.oauth.request.OAuth2RequestFactory;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.store.AuthorizationRequestStore;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OAuth2ClientAwareLoginUrlConverter implements LoginUrlRequestConverter {

    public static final String CLIENT_ID_PARAMETER_NAME = "client_id";
    public static final String AUTHORIZATION_REQUEST_PARAMETER_NAME = "key";

    private final OAuth2ClientDetailsService clientDetailsService;
    private final AuthorizationRequestStore authorizationRequestStore;

    private String loginUrl;
    private OAuth2AuthorizationRequestFactory authorizationRequestFactory;

    public OAuth2ClientAwareLoginUrlConverter(
        OAuth2ClientDetailsService clientDetailsService,
        AuthorizationRequestStore authorizationRequestStore,
        String loginFormUrl
    ) {
        Assert.notNull(clientDetailsService, "clientDetails service is required");
        Assert.notNull(authorizationRequestStore, "authorizationRequest store is required");

        this.clientDetailsService = clientDetailsService;
        this.authorizationRequestStore = authorizationRequestStore;

        this.loginUrl = loginFormUrl;
        OAuth2RequestFactory authRequestFactory = new OAuth2RequestFactory();
        authRequestFactory.setClientDetailsService(clientDetailsService);

        this.authorizationRequestFactory = authRequestFactory;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public void setAuthorizationRequestFactory(OAuth2AuthorizationRequestFactory authorizationRequestFactory) {
        this.authorizationRequestFactory = authorizationRequestFactory;
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

                Map<String, String> parameters = request
                    .getParameterMap()
                    .entrySet()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            e -> e.getKey(),
                            e -> {
                                return e.getValue() != null && e.getValue().length > 0 ? e.getValue()[0] : null;
                            }
                        )
                    );

                try {
                    AuthorizationRequest authorizationRequest = authorizationRequestFactory.createAuthorizationRequest(
                        parameters,
                        clientDetails,
                        null
                    );
                    if (authorizationRequest != null) {
                        String key = authorizationRequestStore.store(authorizationRequest);
                        return "/-/" + realm + loginUrl + "?" + AUTHORIZATION_REQUEST_PARAMETER_NAME + "=" + key;
                    }
                } catch (InvalidRequestException e) {
                    //ignore malformed request
                }

                return "/-/" + realm + loginUrl + "?" + CLIENT_ID_PARAMETER_NAME + "=" + clientId;
            } catch (ClientRegistrationException e) {
                return null;
            }
        }

        // not found
        return null;
    }
}
