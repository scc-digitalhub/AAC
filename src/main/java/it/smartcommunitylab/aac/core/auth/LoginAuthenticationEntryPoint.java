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

package it.smartcommunitylab.aac.core.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/*
 * Custom login entry point to dispatch requests with the proper security context
 */
public class LoginAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    public static final String LOGIN_PATH = "/login";

    public LoginAuthenticationEntryPoint() {
        this(LOGIN_PATH);
    }

    public LoginAuthenticationEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
    }

    @Override
    protected String determineUrlToUseForThisRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) {
        String loginUrl = getLoginFormUrl();

        // extract context (realm, client etc)

        return loginUrl;
    }
}
