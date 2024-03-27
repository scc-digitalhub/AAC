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

package it.smartcommunitylab.aac.password.controller;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.LoginException;
import it.smartcommunitylab.aac.internal.auth.InternalAuthenticationException;
import it.smartcommunitylab.aac.internal.model.InternalLoginProvider;
import it.smartcommunitylab.aac.password.PasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProvider;
import java.util.Collections;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class PasswordLoginController {

    public static final String LOGIN_FORM_URL = PasswordIdentityAuthority.AUTHORITY_URL + "form/{providerId}";

    @Autowired
    private PasswordIdentityAuthority internalAuthority;

    @RequestMapping(value = LOGIN_FORM_URL, method = RequestMethod.GET)
    public String login(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        Model model,
        Locale locale,
        HttpServletRequest req,
        HttpServletResponse res
    ) throws Exception {
        // resolve provider
        PasswordIdentityProvider idp = internalAuthority.getProvider(providerId);
        model.addAttribute("providerId", providerId);

        String realm = idp.getRealm();

        // load realm props
        model.addAttribute("realm", realm);
        model.addAttribute("displayName", realm);

        InternalLoginProvider a = idp.getLoginProvider(null, null);
        // make sure we show the form
        // it should also point to login
        String form = idp.getLoginForm();
        if (form == null) {
            throw new IllegalArgumentException("unsupported-operation");
        }
        a.setTemplate(form);
        a.setLoginUrl(idp.getLoginUrl());
        model.addAttribute("authorities", Collections.singleton(a));

        // check errors
        // we make sure we consume only internal exceptions.
        Exception error = (Exception) req.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (error != null && error instanceof InternalAuthenticationException) {
            LoginException le = LoginException.translate((InternalAuthenticationException) error);

            model.addAttribute("error", le.getError());
            model.addAttribute("errorMessage", le.getMessage());

            // also remove from session
            req.getSession().removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }

        return "login";
    }
}
