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

package it.smartcommunitylab.aac.tos.controller;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.realms.service.RealmService;
import it.smartcommunitylab.aac.tos.TosOnAccessFilter;
import it.smartcommunitylab.aac.users.service.UserService;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping
public class TosController {

    public static final String TOS_TERMS = "/terms";
    public static final String TOS_TERMS_ACCEPT = "/terms/accept";
    public static final String TOS_TERMS_REJECT = "/terms/reject";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private RealmService realmService;

    @Autowired
    private UserService userService;

    @GetMapping("/-/{realm}" + TOS_TERMS)
    public String realmTerms(@PathVariable String realm, Model model) throws NoSuchProviderException {
        Realm realmEntity = realmService.findRealm(realm);
        if (realmEntity == null) {
            throw new NoSuchProviderException("realm not found");
        }
        if (realmEntity.getTosConfiguration() == null || !realmEntity.getTosConfiguration().isEnableTOS()) {
            throw new IllegalArgumentException("tos disabled");
        }

        model.addAttribute("realm", realm);
        model.addAttribute("displayName", realm);
        return "tos/tos";
    }

    @GetMapping(TOS_TERMS)
    public String terms(HttpServletRequest request, Model model, Locale locale) throws NoSuchProviderException {
        UserDetails user = authHelper.getUserDetails();

        if (user == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }

        String realm = user.getRealm();
        Realm realmEntity = realmService.findRealm(realm);

        if (
            realmEntity == null ||
            realmEntity.getTosConfiguration() == null ||
            !realmEntity.getTosConfiguration().isEnableTOS()
        ) {
            throw new NoSuchProviderException("realm not found");
        }

        model.addAttribute("acceptUrl", TOS_TERMS_ACCEPT);
        model.addAttribute("realm", realm);
        model.addAttribute("displayName", realm);
        model.addAttribute("tosForm", "ok");

        if (realmEntity.getTosConfiguration().isApprovedTOS()) {
            model.addAttribute("tosForm", "accept");
        }

        return "tos/tos";
    }

    @PostMapping(TOS_TERMS_ACCEPT)
    public String termsAccepted(
        @RequestParam(required = false, defaultValue = "") String approveParam,
        HttpServletRequest request,
        Model model,
        Locale locale
    ) throws NoSuchUserException, NoSuchProviderException {
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }

        String realm = user.getRealm();
        Realm realmEntity = realmService.findRealm(realm);
        if (
            realmEntity == null ||
            realmEntity.getTosConfiguration() == null ||
            !realmEntity.getTosConfiguration().isEnableTOS()
        ) {
            throw new NoSuchProviderException("realm not found");
        }

        if (approveParam.equals(TosOnAccessFilter.TOS_APRROVED)) {
            request.getSession().setAttribute("termsStatus", approveParam);
            logger.debug("terms of service approved");
            userService.acceptTos(user.getSubjectId());
        } else if (approveParam.equals(TosOnAccessFilter.TOS_REFUSED)) {
            request.getSession().setAttribute("termsStatus", approveParam);
            logger.debug("terms of service rejected");
            userService.rejectTos(user.getSubjectId());
        } else {
            throw new IllegalArgumentException("Need either approve or deny!");
        }

        return "redirect:/";
    }

    @GetMapping("/terms/reject")
    public String rejectTerms(HttpServletRequest request, Model model, Locale locale) {
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }

        return "tos/tos_refuse";
    }
}
