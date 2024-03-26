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

package it.smartcommunitylab.aac.webauthn.controller;

import com.yubico.webauthn.AssertionRequest;
import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.LoginException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.internal.auth.InternalAuthenticationException;
import it.smartcommunitylab.aac.internal.model.InternalLoginProvider;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.realms.RealmManager;
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnAuthenticationStartRequest;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnLoginResponse;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProvider;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnLoginRpService;
import it.smartcommunitylab.aac.webauthn.store.WebAuthnAssertionRequestStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping
public class WebAuthnLoginController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private WebAuthnIdentityAuthority internalAuthority;

    @Autowired
    private RealmManager realmManager;

    @Autowired
    private WebAuthnLoginRpService rpService;

    @Autowired
    private WebAuthnAssertionRequestStore requestStore;

    @RequestMapping(value = WebAuthnIdentityAuthority.AUTHORITY_URL + "form/{providerId}", method = RequestMethod.GET)
    public String login(
        @PathVariable("providerId") String providerId,
        Model model,
        HttpServletRequest req,
        HttpServletResponse res
    ) throws Exception {
        // resolve provider
        WebAuthnIdentityProvider idp = internalAuthority.getProvider(providerId);
        model.addAttribute("providerId", providerId);

        String realm = idp.getRealm();
        model.addAttribute("realm", realm);

        Realm re = realmManager.getRealm(realm);
        String displayName = re.getName();
        Map<String, String> resources = new HashMap<>();
        //        if (!realm.equals(SystemKeys.REALM_COMMON)) {
        //            re = realmManager.getRealm(realm);
        //            displayName = re.getName();
        //            CustomizationBean gcb = re.getCustomization("global");
        //            if (gcb != null) {
        //                resources.putAll(gcb.getResources());
        //            }
        //            // disable realm login customization here,
        //            // we have a per idp message where needed
        ////            CustomizationBean rcb = re.getCustomization("login");
        ////            if (rcb != null) {
        ////                resources.putAll(rcb.getResources());
        ////            }
        //        }

        model.addAttribute("displayName", displayName);
        model.addAttribute("customization", resources);

        InternalLoginProvider a = idp.getLoginProvider(null);
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

    /**
     * Starts a new WebAuthn authentication ceremony by generating a new Credential
     * Request Options object and returning it to the user.
     *
     * The challenge and the information about the ceremony are temporarily stored
     * in the session.
     *
     * @throws NoSuchProviderException
     * @throws NoSuchUserException
     */
    @Hidden
    @PostMapping(
        value = WebAuthnIdentityAuthority.AUTHORITY_URL + "assertionOptions/{providerId}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebAuthnLoginResponse> generateAssertionOptions(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        @RequestBody @Valid WebAuthnAuthenticationStartRequest body
    ) throws NoSuchProviderException, NoSuchUserException {
        try {
            // build request for user
            String username = body.getUsername();
            // TODO evaluate displayName support

            logger.debug("build login assertionOptions for user {}", StringUtils.trimAllWhitespace(username));
            AssertionRequest assertionRequest = rpService.startLogin(providerId, username);

            // store request
            String key = requestStore.store(assertionRequest);
            if (logger.isTraceEnabled()) {
                logger.trace("assertion {}: {}", key, String.valueOf(assertionRequest));
            }

            // build response
            WebAuthnLoginResponse response = new WebAuthnLoginResponse();
            response.setAssertionRequest(assertionRequest);
            response.setKey(key);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.debug("error in assertion options: " + e.getMessage());

            // respond with a generic error
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
