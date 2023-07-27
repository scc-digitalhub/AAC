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

import it.smartcommunitylab.aac.core.entrypoint.RealmAwarePathUriBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class ExtendedLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    public static final String REDIRECT_ATTRIBUTE = "redirect";

    private String loginUrl;
    public RealmAwarePathUriBuilder realmUriBuilder;

    public ExtendedLogoutSuccessHandler(String loginUrl) {
        Assert.hasText(loginUrl, "login url is required");
        this.loginUrl = loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public void setRealmUriBuilder(RealmAwarePathUriBuilder realmUriBuilder) {
        this.realmUriBuilder = realmUriBuilder;
    }

    /*
     * Redirect user to realm login, ignoring parameters referrals etc
     */
    @Override
    protected String determineTargetUrl(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) {
        // check if attribute
        if (request.getAttribute(REDIRECT_ATTRIBUTE) != null) {
            String redirectUrl = (String) request.getAttribute(REDIRECT_ATTRIBUTE);
            if (StringUtils.hasText(redirectUrl)) {
                return redirectUrl;
            }
        }

        // fallback to login page
        if (authentication != null && authentication instanceof UserAuthentication) {
            UserAuthentication userAuth = (UserAuthentication) authentication;
            String realm = userAuth.getRealm();

            return buildLoginUrl(request, realm);
        }

        return super.determineTargetUrl(request, response, authentication);
    }

    private String buildLoginUrl(HttpServletRequest request, String realm) {
        if (realmUriBuilder != null) {
            return realmUriBuilder.buildUri(request, realm, loginUrl).toUriString();
        }

        return "/-/" + realm + loginUrl;
    }
}
