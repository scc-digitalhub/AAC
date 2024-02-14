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

package it.smartcommunitylab.aac.openidfed.controller;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.LoginException;
import it.smartcommunitylab.aac.openidfed.OpenIdFedIdentityAuthority;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProvider;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedLoginProvider;
import java.util.Collections;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class OpenIdFedLoginController {

    public static final String LOGIN_FORM_URL = OpenIdFedIdentityAuthority.AUTHORITY_URL + "form/{providerId}";

    @Autowired
    private OpenIdFedIdentityAuthority openidfedAuthority;

    @RequestMapping(value = LOGIN_FORM_URL, method = RequestMethod.GET)
    public String login(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        Model model,
        Locale locale,
        HttpServletRequest req,
        HttpServletResponse res
    ) throws Exception {
        // resolve provider
        OpenIdFedIdentityProvider idp = openidfedAuthority.getProvider(providerId);
        model.addAttribute("providerId", providerId);

        String realm = idp.getRealm();

        // load realm props
        model.addAttribute("realm", realm);
        model.addAttribute("displayName", realm);

        OpenIdFedLoginProvider a = idp.getLoginProvider();
        model.addAttribute("authorities", Collections.singleton(a));

        // check errors
        // we make sure we consume only internal exceptions.
        Exception error = (Exception) req.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (error != null && error instanceof OAuth2AuthenticationException) {
            LoginException le = LoginException.translate((OAuth2AuthenticationException) error);

            model.addAttribute("error", le.getError());
            model.addAttribute("errorMessage", le.getMessage());

            // also remove from session
            req.getSession().removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }

        return "login";
    }
}
