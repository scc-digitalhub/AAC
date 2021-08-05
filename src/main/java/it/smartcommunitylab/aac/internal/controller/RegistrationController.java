/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.internal.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.dto.CustomizationBean;
import it.smartcommunitylab.aac.dto.UserRegistrationBean;
import it.smartcommunitylab.aac.dto.UserResetBean;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProvider;
import it.smartcommunitylab.aac.internal.provider.InternalPasswordService;
import it.smartcommunitylab.aac.model.Realm;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author raman
 *
 */
@Controller
@RequestMapping
public class RegistrationController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProviderManager providerManager;

    @Autowired
    private RealmManager realmManager;

    /**
     * Redirect to registration page
     */
    @ApiIgnore
    @RequestMapping(value = "/auth/internal/register/{providerId}", method = RequestMethod.GET)
    public String registrationPage(
            @PathVariable("providerId") String providerId,
            Model model) throws NoSuchProviderException, NoSuchRealmException {

        // resolve provider
        IdentityService ids = providerManager.getIdentityService(providerId);

        if (!ids.canRegister()) {
            throw new RegistrationException("registration is disabled");
        }

        model.addAttribute("providerId", providerId);

        String realm = ids.getRealm();
        model.addAttribute("realm", realm);

        Realm re = realmManager.getRealm(realm);
        String displayName = re.getName();
        Map<String, String> resources = new HashMap<>();
        if (!realm.equals(SystemKeys.REALM_COMMON)) {
            re = realmManager.getRealm(realm);
            displayName = re.getName();
            CustomizationBean gcb = re.getCustomization("global");
            if (gcb != null) {
                resources.putAll(gcb.getResources());
            }
            CustomizationBean rcb = re.getCustomization("registration");
            if (rcb != null) {
                resources.putAll(rcb.getResources());
            }
        }

        model.addAttribute("displayName", displayName);
        model.addAttribute("customization", resources);

        // build model
        model.addAttribute("reg", new UserRegistrationBean());

//        // check if we have a clientId
//        String clientId = (String) req.getSession().getAttribute(OAuth2Utils.CLIENT_ID);
//        if (clientId != null) {
//            // fetch client customizations for login screen
//            Map<String, String> customizations = clientDetailsAdapter.getClientCustomizations(clientId);
//            model.addAllAttributes(customizations);
//        }

        // build url
        // TODO handle via urlBuilder or entryPoint
        model.addAttribute("registrationUrl", "/auth/internal/register/" + providerId);
        model.addAttribute("loginUrl", "/-/" + realm + "/login");

        return "registration/register";
    }

    /**
     * Register the user and redirect to the 'registersuccess' page
     */
    @ApiIgnore
    @RequestMapping(value = "/auth/internal/register/{providerId}", method = RequestMethod.POST)
    public String register(Model model,
            @PathVariable("providerId") String providerId,
            @ModelAttribute("reg") @Valid UserRegistrationBean reg,
            BindingResult result,
            HttpServletRequest req) {

        try {

            // resolve provider
            IdentityService ids = providerManager.getIdentityService(providerId);

            if (!ids.canRegister()) {
                throw new RegistrationException("registration is disabled");
            }

            String realm = ids.getRealm();

            // build model for result
            model.addAttribute("providerId", providerId);
            model.addAttribute("realm", realm);

            Realm re = realmManager.getRealm(realm);
            String displayName = re.getName();
            Map<String, String> resources = new HashMap<>();
            if (!realm.equals(SystemKeys.REALM_COMMON)) {
                re = realmManager.getRealm(realm);
                displayName = re.getName();
                CustomizationBean gcb = re.getCustomization("global");
                if (gcb != null) {
                    resources.putAll(gcb.getResources());
                }
                CustomizationBean rcb = re.getCustomization("registration");
                if (rcb != null) {
                    resources.putAll(rcb.getResources());
                }
            }

            model.addAttribute("displayName", displayName);
            model.addAttribute("customization", resources);

            model.addAttribute("registrationUrl", "/auth/internal/register/" + providerId);
            model.addAttribute("loginUrl", "/-/" + realm + "/login");

            if (result.hasErrors()) {
                return "registration/register";
            }

            String username = reg.getEmail();
            String password = reg.getPassword();
            String email = reg.getEmail();
            String name = reg.getName();
            String surname = reg.getSurname();
            String lang = reg.getLang();

            // convert registration model to internal model
            InternalUserAccount account = new InternalUserAccount();
            account.setUsername(username);
            account.setPassword(password);
            account.setEmail(email);
            account.setName(name);
            account.setSurname(surname);
            account.setLang(lang);

            // TODO handle additional attributes from registration

            // TODO handle subject resolution to link with existing accounts
            // either via current session or via providers from same realm
            String subjectId = null;

            // register
            UserIdentity identity = ids.registerIdentity(subjectId, account, Collections.emptyList());

            model.addAttribute("identity", identity);

            // WRONG, should send redirect to success page to avoid double POST
            return "registration/regsuccess";
        } catch (RegistrationException e) {
            model.addAttribute("error", e.getClass().getSimpleName());
            return "registration/register";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            model.addAttribute("error", RegistrationException.class.getSimpleName());
            return "registration/register";
        }
    }

    /**
     * Register with the REST call
     * 
     * TODO move to a proper API controller, supporting all providers
     */
//    @RequestMapping(value = "/internal/register/rest", method = RequestMethod.POST)
//    public @ResponseBody void registerREST(@RequestBody UserRegistrationBean reg,
//            HttpServletResponse res) {
//        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
//        Validator validator = factory.getValidator();
//        Set<ConstraintViolation<UserRegistrationBean>> errors = validator.validate(reg);
//
//        if (errors.size() > 0) {
//            res.setStatus(HttpStatus.BAD_REQUEST.value());
//            return;
//        }
//        try {
//            // register internal identity
//            // TODO map realm
//            // TODO fetch existing subject for account linking
//            // for now generate subject here
//            String subject = UUID.randomUUID().toString();
//            String realm = "";
//
//            InternalUserAccount user = userManager.registerAccount(subject, realm, null, reg.getPassword(),
//                    reg.getEmail(), reg.getName(),
//                    reg.getSurname(), reg.getLang(), null);
//
//            String userId = user.getUserId();
//
//            // check confirmation
//            if (confirmationRequired) {
//                // generate confirmation keys and send mail
//                userManager.resetConfirmation(subject, realm, userId, true);
//            } else {
//                // auto approve
//                userManager.approveConfirmation(subject, realm, userId);
//            }
//        } catch (AlreadyRegisteredException e) {
//            res.setStatus(HttpStatus.CONFLICT.value());
//        } catch (RegistrationException e) {
//            logger.error(e.getMessage(), e);
//            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
//        }
//    }

//    /**
//     * Redirect to the resend page to ask for the email
//     * 
//     * @param model
//     * @param username
//     * @return
//     */
//    @ApiIgnore
//    @RequestMapping(value = "/internal/resend")
//    public String resendPage() {
//        return "registration/resend";
//    }

//    /**
//     * Resend the confirmation link to the registered user.
//     * 
//     * @param model
//     * @param username
//     * @return
//     */
//    @ApiIgnore
//    @RequestMapping(value = "/internal/resend", method = RequestMethod.POST)
//    public String resendConfirm(Model model, @RequestParam String username) {
//        try {
//            regService.resendConfirm(username);
//            return "registration/regsuccess";
//        } catch (RegistrationException e) {
//            logger.error(e.getMessage(), e);
//            model.addAttribute("error", e.getClass().getSimpleName());
//            return "registration/resend";
//        }
//    }

//    /**
//     * Confirm the user given the confirmation code sent via mail. Redirect to
//     * confirmsuccess page
//     * 
//     * @param model
//     * @param confirmationCode
//     * @return
//     */
//    @ApiIgnore
//    @RequestMapping("/internal/confirm")
//    public String confirm(Model model, @RequestParam String confirmationCode,
//            @RequestParam(required = false) Boolean reset, HttpServletRequest req) {
//        if (Boolean.TRUE.equals(reset)) {
//            try {
//                Registration user = regService.getUserByPwdResetToken(confirmationCode);
//                req.getSession().setAttribute("changePwdEmail", user.getEmail());
//                model.addAttribute("reg", new RegistrationBean());
//                return "registration/changepwd";
//            } catch (RegistrationException e) {
//                e.printStackTrace();
//                model.addAttribute("error", e.getClass().getSimpleName());
//                return "registration/confirmerror";
//            }
//        } else {
//            try {
//                Registration user = regService.confirm(confirmationCode);
//                if (!user.isChangeOnFirstAccess()) {
//                    return "registration/confirmsuccess";
//                } else {
//                    req.getSession().setAttribute("changePwdEmail", user.getEmail());
//                    model.addAttribute("reg", new RegistrationBean());
//                    return "registration/changepwd";
//                }
//            } catch (RegistrationException e) {
//                e.printStackTrace();
//                model.addAttribute("error", e.getClass().getSimpleName());
//                return "registration/confirmerror";
//            }
//        }
//
//    }

    @ApiIgnore
    @RequestMapping(value = "/auth/internal/reset/{providerId}", method = RequestMethod.GET)
    public String resetPage(
            @PathVariable("providerId") String providerId,
            Model model) throws NoSuchProviderException, NoSuchRealmException {

        // resolve provider
        IdentityService ids = providerManager.getIdentityService(providerId);

        if (!ids.getCredentialsService().canReset()) {
            throw new RegistrationException("reset is disabled");
        }

        model.addAttribute("providerId", providerId);

        String realm = ids.getRealm();
        model.addAttribute("realm", realm);

        String displayName = null;
        Realm re = null;
        Map<String, String> resources = new HashMap<>();
        if (!realm.equals(SystemKeys.REALM_COMMON)) {
            re = realmManager.getRealm(realm);
            displayName = re.getName();
            CustomizationBean gcb = re.getCustomization("global");
            if (gcb != null) {
                resources.putAll(gcb.getResources());
            }
            CustomizationBean lcb = re.getCustomization("login");
            if (lcb != null) {
                resources.putAll(lcb.getResources());
            }
        }

        model.addAttribute("displayName", displayName);
        model.addAttribute("customization", resources);

        // build model
        model.addAttribute("reg", new UserResetBean());

        // build url
        // TODO handle via urlBuilder or entryPoint
        model.addAttribute("resetUrl", "/auth/internal/reset/" + providerId);
        model.addAttribute("loginUrl", "/-/" + realm + "/login");

        return "registration/resetpwd";
    }

    @ApiIgnore
    @RequestMapping(value = "/auth/internal/reset/{providerId}", method = RequestMethod.POST)
    public String reset(Model model,
            @PathVariable("providerId") String providerId,
            @ModelAttribute("reg") @Valid UserResetBean reg,
            BindingResult result,
            HttpServletRequest req) {

        if (result.hasErrors()) {
            return "registration/resetpwd";
        }

        try {

            // resolve provider
            IdentityService ids = providerManager.getIdentityService(providerId);

            // shortcut, we use only internal provider in this path
            if (!(ids instanceof InternalIdentityProvider)) {
                throw new RegistrationException("reset is not supported");
            }

            InternalIdentityProvider ip = (InternalIdentityProvider) ids;

            String realm = ids.getRealm();

            // build model for result
            model.addAttribute("providerId", providerId);
            model.addAttribute("realm", realm);
            model.addAttribute("resetUrl", "/auth/internal/reset/" + providerId);
            model.addAttribute("loginUrl", "/-/" + realm + "/login");

            String displayName = null;
            Realm re = null;
            Map<String, String> resources = new HashMap<>();
            if (!realm.equals(SystemKeys.REALM_COMMON)) {
                re = realmManager.getRealm(realm);
                displayName = re.getName();
                CustomizationBean gcb = re.getCustomization("global");
                if (gcb != null) {
                    resources.putAll(gcb.getResources());
                }
                CustomizationBean lcb = re.getCustomization("login");
                if (lcb != null) {
                    resources.putAll(lcb.getResources());
                }
            }

            model.addAttribute("displayName", displayName);
            model.addAttribute("customization", resources);

            String username = reg.getUsername();
            String email = reg.getEmail();

            Map<String, String> attributes = new HashMap<>();
            attributes.put("realm", realm);
            attributes.put("provider", providerId);

            if (StringUtils.hasText(username)) {
                attributes.put("username", username);
            }
            if (StringUtils.hasText(email)) {
                attributes.put("email", email);
            }

            // resolve user
            UserAccount account = ip.getAccountProvider().getByIdentifyingAttributes(attributes);
            String userId = account.getUserId();

            // direct call to reset
            InternalPasswordService passwordService = (InternalPasswordService) ip.getCredentialsService();
            account = passwordService.resetPassword(userId);

            model.addAttribute("account", account);

            // WRONG, should send redirect to success page to avoid double POST
            return "registration/resetsuccess";
        } catch (RegistrationException e) {
            model.addAttribute("error", e.getMessage());
            return "registration/resetpwd";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            model.addAttribute("error", RegistrationException.class.getSimpleName());
            return "registration/resetpwd";
        }
    }

//    @ApiIgnore
//    @RequestMapping(value = "/internal/reset", method = RequestMethod.POST)
//    public String reset(Model model, @RequestParam String username) {
//        try {
//            regService.resetPassword(username);
//        } catch (RegistrationException e) {
//            model.addAttribute("error", e.getClass().getSimpleName());
//            return "registration/resetpwd";
//        }
//        return "registration/resetsuccess";
//    }
//
//    @ApiIgnore
//    @RequestMapping(value = "/internal/changepwd", method = RequestMethod.POST)
//    public String changePwd(Model model,
//            @ModelAttribute("reg") @Valid RegistrationBean reg,
//            BindingResult result,
//            HttpServletRequest req) {
//        if (result.hasFieldErrors("password")) {
//            return "registration/changepwd";
//        }
//        String userMail = (String) req.getSession().getAttribute("changePwdEmail");
//        if (userMail == null) {
//            model.addAttribute("error", RegistrationException.class.getSimpleName());
//            return "registration/changepwd";
//        }
//        req.getSession().removeAttribute("changePwdEmail");
//
//        try {
//            regService.updatePassword(userMail, reg.getPassword());
//        } catch (RegistrationException e) {
//            logger.error(e.getMessage(), e);
//            model.addAttribute("error", e.getClass().getSimpleName());
//            return "registration/changepwd";
//        }
//        return "registration/changesuccess";
//    }
}
