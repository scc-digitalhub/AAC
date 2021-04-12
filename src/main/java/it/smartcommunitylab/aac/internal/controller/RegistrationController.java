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

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
//import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.SessionManager;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.dto.UserRegistrationBean;
import it.smartcommunitylab.aac.internal.InternalUserManager;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author raman
 *
 */
@Controller
@RequestMapping
public class RegistrationController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${authorities.internal.confirmation.required}")
    private boolean confirmationRequired;

    @Value("${authorities.internal.password.reset.enabled}")
    private boolean passwordResetEnabled;

    @Value("${authorities.internal.linking}")
    private boolean accountLinking;

    @Autowired
    private AuthenticationHelper sessionManager;

//    @Autowired
//    private RegistrationService regService;
//
//    @Autowired
//    private AttributesAdapter attributesAdapter;

    @Autowired
    private InternalUserManager userManager;

    /**
     * Redirect to registration page
     * 
     * @param model
     * @param req
     * @return
     */
    @ApiIgnore
    @RequestMapping("/internal/register")
    public String regPage(Model model,
            HttpServletRequest req) {
        model.addAttribute("reg", new UserRegistrationBean());

//        // check if we have a clientId
//        String clientId = (String) req.getSession().getAttribute(OAuth2Utils.CLIENT_ID);
//        if (clientId != null) {
//            // fetch client customizations for login screen
//            Map<String, String> customizations = clientDetailsAdapter.getClientCustomizations(clientId);
//            model.addAllAttributes(customizations);
//        }
        return "registration/register";
    }

    /**
     * Register the user and redirect to the 'registersuccess' page
     * 
     * @param model
     * @param reg
     * @param result
     * @param req
     * @return
     */
    @ApiIgnore
    @RequestMapping(value = "/internal/register", method = RequestMethod.POST)
    public String register(Model model,
            @ModelAttribute("reg") @Valid UserRegistrationBean reg,
            BindingResult result,
            HttpServletRequest req) {
        if (result.hasErrors()) {
            HttpSession ss = req.getSession();
            return "registration/register";
        }
        try {

            // register internal identity
            // TODO map realm
            // generate subject here
            String subject = UUID.randomUUID().toString();
            if (accountLinking) {
                // fetch existing subject for account linking
                UserAuthenticationToken auth = sessionManager.getUserAuthentication();
                if (auth != null) {
                    // TODO check if already logged via internal, can't bind internal accounts
                    // link to session
                    subject = auth.getSubject().getSubjectId();
                }
            }
            String realm = "";

            InternalUserAccount user = userManager.registerAccount(subject, realm, null, reg.getPassword(),
                    reg.getEmail(), reg.getName(),
                    reg.getSurname(), reg.getLang(), null);

            String userId = user.getUserId();

            // check confirmation
            if (confirmationRequired) {
                // generate confirmation keys and send mail
                userManager.resetConfirmation(subject, realm, userId, true);
            } else {
                // auto approve
                userManager.approveConfirmation(subject, realm, userId);
            }

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
     * @param model
     * @param reg
     * @param result
     * @param req
     * @return
     */
    @RequestMapping(value = "/internal/register/rest", method = RequestMethod.POST)
    public @ResponseBody void registerREST(@RequestBody UserRegistrationBean reg,
            HttpServletResponse res) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<UserRegistrationBean>> errors = validator.validate(reg);

        if (errors.size() > 0) {
            res.setStatus(HttpStatus.BAD_REQUEST.value());
            return;
        }
        try {
            // register internal identity
            // TODO map realm
            // TODO fetch existing subject for account linking
            // for now generate subject here
            String subject = UUID.randomUUID().toString();
            String realm = "";

            InternalUserAccount user = userManager.registerAccount(subject, realm, null, reg.getPassword(),
                    reg.getEmail(), reg.getName(),
                    reg.getSurname(), reg.getLang(), null);

            String userId = user.getUserId();

            // check confirmation
            if (confirmationRequired) {
                // generate confirmation keys and send mail
                userManager.resetConfirmation(subject, realm, userId, true);
            } else {
                // auto approve
                userManager.approveConfirmation(subject, realm, userId);
            }
        } catch (AlreadyRegisteredException e) {
            res.setStatus(HttpStatus.CONFLICT.value());
        } catch (RegistrationException e) {
            logger.error(e.getMessage(), e);
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    /**
     * Redirect to the resend page to ask for the email
     * 
     * @param model
     * @param username
     * @return
     */
    @ApiIgnore
    @RequestMapping(value = "/internal/resend")
    public String resendPage() {
        return "registration/resend";
    }

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
    @RequestMapping(value = "/internal/reset", method = RequestMethod.GET)
    public String resetPage() {
        return "registration/resetpwd";
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
