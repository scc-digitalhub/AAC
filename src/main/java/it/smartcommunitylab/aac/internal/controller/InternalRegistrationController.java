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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.dto.CustomizationBean;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;
import it.smartcommunitylab.aac.internal.dto.UserRegistrationBean;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalAccountService;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityService;
import it.smartcommunitylab.aac.model.Realm;

/**
 * @author raman
 *
 */
@Controller
@RequestMapping
public class InternalRegistrationController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private InternalIdentityAuthority internalAuthority;

    @Autowired
    private RealmManager realmManager;

    @Autowired
    private MessageSource messageSource;

    /*
     * Edit account page
     */
    @GetMapping("/changeaccount/{providerId}/{uuid}")
    public String changeaccount(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid,
            HttpServletRequest request,
            Model model)
            throws NoSuchProviderException, NoSuchUserException, NoSuchRealmException {

        // first check userid vs user
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }

        // fetch internal identities matching provider
        Set<InternalUserIdentity> identities = user.getIdentities().stream()
                .filter(i -> SystemKeys.AUTHORITY_INTERNAL.equals(i.getAuthority())
                        && i.getProvider().equals(providerId))
                .map(i -> (InternalUserIdentity) i)
                .collect(Collectors.toSet());

        // pick matching by uuid
        InternalUserIdentity identity = identities.stream()
                .filter(i -> i.getAccount().getUuid().equals(uuid))
                .findFirst().orElse(null);
        if (identity == null) {
            throw new IllegalArgumentException("error.invalid_user");
        }

        String userId = identity.getUserId();
        InternalUserAccount account = identity.getAccount();

        // fetch provider
        InternalIdentityService idp = internalAuthority.getProvider(providerId);
        if (!idp.getConfig().isEnableUpdate()) {
            throw new IllegalArgumentException("error.unsupported_operation");
        }

        String realm = idp.getRealm();
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

        // for internal username is accountId
        String username = account.getAccountId();
        UserRegistrationBean reg = new UserRegistrationBean();
        reg.setEmail(account.getEmail());
        reg.setName(account.getName());
        reg.setSurname(account.getSurname());
        reg.setLang(account.getLang());

        // build model
        model.addAttribute("userId", userId);
        model.addAttribute("username", account.getUsername());
        model.addAttribute("uuid", account.getUuid());
        model.addAttribute("account", account);
        model.addAttribute("reg", reg);
        model.addAttribute("accountUrl", "/account");
        model.addAttribute("changeUrl", "/changeaccount/" + providerId + "/" + uuid);

        return "registration/changeaccount";
    }

    @PostMapping("/changeaccount/{providerId}/{uuid}")
    public String changeaccount(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid,
            Model model,
            @ModelAttribute("reg") @Valid UserRegistrationBean reg,
            HttpServletRequest request,
            BindingResult result)
            throws NoSuchProviderException, NoSuchUserException {

        try {
            // first check userid vs user
            UserDetails user = authHelper.getUserDetails();
            if (user == null) {
                throw new InsufficientAuthenticationException("error.unauthenticated_user");
            }

            // fetch internal identities matching provider
            Set<InternalUserIdentity> identities = user.getIdentities().stream()
                    .filter(i -> SystemKeys.AUTHORITY_INTERNAL.equals(i.getAuthority())
                            && i.getProvider().equals(providerId))
                    .map(i -> (InternalUserIdentity) i)
                    .collect(Collectors.toSet());

            // pick matching by username
            InternalUserIdentity identity = identities.stream()
                    .filter(i -> i.getAccount().getUuid().equals(uuid))
                    .findFirst().orElse(null);
            if (identity == null) {
                throw new IllegalArgumentException("error.invalid_user");
            }

            String userId = identity.getUserId();
            InternalUserAccount account = identity.getAccount();

            // fetch provider
            InternalIdentityService idp = internalAuthority.getProvider(providerId);
            if (!idp.getConfig().isEnableUpdate()) {
                throw new IllegalArgumentException("error.unsupported_operation");
            }

            InternalAccountService service = idp.getAccountService();

            String realm = idp.getRealm();
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

            // for internal username is accountId
            String username = account.getAccountId();

            // extract props
            String email = reg.getEmail();
            String name = reg.getName();
            String surname = reg.getSurname();
            String lang = reg.getLang();

            // update
            account.setEmail(email);
            account.setName(name);
            account.setSurname(surname);
            account.setLang(lang);

            // build model
            model.addAttribute("userId", userId);
            model.addAttribute("username", account.getUsername());
            model.addAttribute("uuid", account.getUuid());
            model.addAttribute("account", account);
            model.addAttribute("accountUrl", "/account");
            model.addAttribute("changeUrl", "/changeaccount/" + providerId + "/" + uuid);

            if (result.hasErrors()) {
                return "registration/changeaccount";
            }

            account = service.updateAccount(username, account);
            model.addAttribute("account", account);

            return "registration/changesuccess";
        } catch (RegistrationException e) {
            model.addAttribute("error", e.getMessage());
            return "registration/changeaccount";
        } catch (Exception e) {
            model.addAttribute("error", RegistrationException.ERROR);
            return "registration/changeaccount";
        }
    }

    /**
     * Redirect to registration page
     */
    @Hidden
    @RequestMapping(value = "/auth/internal/register/{providerId}", method = RequestMethod.GET)
    public String registrationPage(
            @PathVariable("providerId") String providerId,
            Model model) throws NoSuchProviderException, NoSuchRealmException {

        // resolve provider
        InternalIdentityService idp = internalAuthority.getProvider(providerId);
        if (!idp.getConfig().isEnableRegistration()) {
            throw new RegistrationException("unsupported_operation");
        }

        model.addAttribute("providerId", providerId);

        String realm = idp.getRealm();
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
        // set idp form as login url
        model.addAttribute("loginUrl", "/auth/internal/form/" + providerId);

        return "registration/register";
    }

    /**
     * Register the user and redirect to the 'registersuccess' page
     */
    @Hidden
    @RequestMapping(value = "/auth/internal/register/{providerId}", method = RequestMethod.POST)
    public String register(Model model, Locale locale,
            @PathVariable("providerId") String providerId,
            @ModelAttribute("reg") @Valid UserRegistrationBean reg,
            BindingResult result,
            HttpServletRequest req) {

        try {

            // resolve provider
            InternalIdentityService idp = internalAuthority.getProvider(providerId);

            if (!idp.getConfig().isEnableRegistration()) {
                throw new RegistrationException("unsupported_operation");
            }

            String realm = idp.getRealm();

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

            // set idp form as login url
            model.addAttribute("loginUrl", "/auth/internal/form/" + providerId);

            if (result.hasErrors()) {
                model.addAttribute("error", InvalidDataException.ERROR);
                // check if custom msg available
                Optional<FieldError> fieldError = Optional.ofNullable(result.getFieldError());
                if (fieldError.isPresent()) {
                    String errorMsg = fieldError.get().getDefaultMessage();

                    if (errorMsg != null && errorMsg.startsWith("error.")) {
                        model.addAttribute("error", errorMsg);
                    }
                }

                return "registration/register";
            }

            String username = reg.getEmail();
            String email = reg.getEmail();
            String name = reg.getName();
            String surname = reg.getSurname();
            String lang = reg.getLang() != null ? reg.getLang() : locale.getLanguage();

            // convert registration model to internal model
            InternalUserAccount account = new InternalUserAccount(idp.getAuthority());
            account.setUsername(username);
            account.setEmail(email);
            account.setName(name);
            account.setSurname(surname);
            account.setLang(lang);

            // TODO handle additional attributes from registration

            // TODO handle subject resolution to link with existing accounts
            // either via current session or via providers from same realm
            // TODO force require confirmation if linking
            String subjectId = null;

            // build reg model
            InternalUserIdentity identity = new InternalUserIdentity(idp.getAuthority(), idp.getProvider(),
                    idp.getRealm(), account);

            // register
            identity = idp.registerIdentity(subjectId, identity);

            model.addAttribute("identity", identity);

            return "registration/regsuccess";
        } catch (InvalidDataException | MissingDataException | DuplicatedDataException e) {
            StringBuilder msg = new StringBuilder();
            msg.append(messageSource.getMessage(e.getMessage(), null, req.getLocale()));
            msg.append(": ");
            msg.append(messageSource.getMessage("field." + e.getField(), null, req.getLocale()));

            model.addAttribute("error", msg.toString());
            return "attributes/form";
        } catch (RegistrationException e) {
            model.addAttribute("error", e.getMessage());
            return "registration/register";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            model.addAttribute("error", RegistrationException.ERROR);
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
