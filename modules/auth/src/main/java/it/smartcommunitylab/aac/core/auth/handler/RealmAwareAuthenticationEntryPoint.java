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

package it.smartcommunitylab.aac.core.auth.handler;

import it.smartcommunitylab.aac.SystemKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

public class RealmAwareAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    public static final String REALM_URI_VARIABLE_NAME = "realm";

    private RequestMatcher realmRequestMatcher;
    private RequestMatcher devRequestMatcher;
    public RealmAwareUriBuilder realmUriBuilder;

    public RealmAwareAuthenticationEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
        // build a matcher for realm requests
        realmRequestMatcher = new AntPathRequestMatcher("/-/{" + REALM_URI_VARIABLE_NAME + "}/**");
        devRequestMatcher = new AntPathRequestMatcher("/dev/**");
    }

    public void setRealmRequestMatcher(RequestMatcher realmRequestMatcher) {
        this.realmRequestMatcher = realmRequestMatcher;
    }

    public void setRealmUriBuilder(RealmAwareUriBuilder realmUriBuilder) {
        this.realmUriBuilder = realmUriBuilder;
    }

    @Override
    protected String determineUrlToUseForThisRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) {
        //        System.out.println("request for " + String.valueOf(request.getRequestURI()));

        // check via matcher
        if (realmRequestMatcher.matches(request)) {
            // resolve realm
            String realm = realmRequestMatcher.matcher(request).getVariables().get(REALM_URI_VARIABLE_NAME);

            return buildLoginUrl(request, realm);
        }

        // check in parameters
        if (StringUtils.hasText(request.getParameter(REALM_URI_VARIABLE_NAME))) {
            String realm = request.getParameter(REALM_URI_VARIABLE_NAME);
            return buildLoginUrl(request, realm);
        }

        // check in attributes
        if (StringUtils.hasText((String) request.getAttribute(REALM_URI_VARIABLE_NAME))) {
            String realm = (String) request.getAttribute(REALM_URI_VARIABLE_NAME);
            return buildLoginUrl(request, realm);
        }

        // check if dev console, system realm
        if (devRequestMatcher.matches(request)) {
            return buildLoginUrl(request, SystemKeys.REALM_SYSTEM);
        }

        // return global
        return getLoginFormUrl();
    }

    private String buildLoginUrl(HttpServletRequest request, String realm) {
        if (realmUriBuilder != null) {
            return realmUriBuilder.buildUri(request, realm, getLoginFormUrl()).toUriString();
        }

        return "/-/" + realm + getLoginFormUrl();
    }
}
