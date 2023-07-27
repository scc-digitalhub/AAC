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

package it.smartcommunitylab.aac.openid.auth;

import it.smartcommunitylab.aac.SystemKeys;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class OIDCAuthenticationException extends AuthenticationException {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    private final OAuth2Error error;
    private final String authorizationRequest;
    private final String authorizationResponse;
    private final String tokenRequest;
    private final String tokenResponse;

    public OIDCAuthenticationException(OAuth2Error error) {
        this(error, error.getDescription());
    }

    public OIDCAuthenticationException(OAuth2Error error, String message) {
        super(message);
        this.error = error;
        authorizationRequest = null;
        authorizationResponse = null;
        tokenRequest = null;
        tokenResponse = null;
    }

    public OIDCAuthenticationException(
        OAuth2Error error,
        String message,
        String authorizationRequest,
        String authorizationResponse,
        String tokenRequest,
        String tokenResponse
    ) {
        super(message);
        this.error = error;
        this.authorizationRequest = authorizationRequest;
        this.authorizationResponse = authorizationResponse;
        this.tokenRequest = tokenRequest;
        this.tokenResponse = tokenResponse;
    }

    public OAuth2Error getError() {
        return error;
    }

    public String getErrorMessage() {
        return "oauth2.error." + getError().getErrorCode();
    }

    public String getAuthorizationRequest() {
        return authorizationRequest;
    }

    public String getAuthorizationResponse() {
        return authorizationResponse;
    }

    public String getTokenRequest() {
        return tokenRequest;
    }

    public String getTokenResponse() {
        return tokenResponse;
    }
}
