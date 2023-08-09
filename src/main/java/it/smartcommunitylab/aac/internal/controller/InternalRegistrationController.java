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
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.credentials.provider.AccountCredentialsService;
import it.smartcommunitylab.aac.internal.InternalIdentityServiceAuthority;
import it.smartcommunitylab.aac.internal.dto.UserRegistrationBean;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityService;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.password.model.PasswordPolicy;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.provider.PasswordCredentialsService;
import java.nio.file.ProviderNotFoundException;
import java.util.Collections;
import java.util.Locale;
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
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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
    private InternalIdentityServiceAuthority internalAuthority;

    @Autowired
    private RealmService realmService;

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
        Model model,
        Locale locale
    ) throws NoSuchProviderException, NoSuchUserException, NoSuchRealmException {
        // first check userid vs user
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }

        // fetch internal identities matching provider
        Set<InternalUserIdentity> identities = user
            .getIdentities()
            .stream()
            .filter(i -> SystemKeys.AUTHORITY_INTERNAL.equals(i.getAuthority()) && i.getProvider().equals(providerId))
            .map(i -> (InternalUserIdentity) i)
            .collect(Collectors.toSet());

        // pick matching by uuid
        InternalUserIdentity identity = identities
            .stream()
            .filter(i -> i.getAccount().getUuid().equals(uuid))
            .findFirst()
            .orElse(null);
        if (identity == null) {
            throw new IllegalArgumentException("error.invalid_user");
        }

        String userId = identity.getUserId();
        InternalUserAccount account = identity.getAccount();

        // fetch provider
        InternalIdentityService service = internalAuthority.getProvider(providerId);
        if (!service.getConfig().isEnableUpdate()) {
            throw new IllegalArgumentException("error.unsupported_operation");
        }

        // load realm props
        String realm = service.getRealm();
        model.addAttribute("realm", realm);
        model.addAttribute("displayName", realm);

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

        return "internal/changeaccount";
    }

    @PostMapping("/changeaccount/{providerId}/{uuid}")
    public String changeaccount(
        @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid,
        Model model,
        Locale locale,
        @ModelAttribute("reg") @Valid UserRegistrationBean reg,
        BindingResult result,
        HttpServletRequest request
    ) throws NoSuchProviderException, NoSuchUserException {
        try {
            // first check userid vs user
            UserDetails user = authHelper.getUserDetails();
            if (user == null) {
                throw new InsufficientAuthenticationException("error.unauthenticated_user");
            }

            // fetch internal identities matching provider
            Set<InternalUserIdentity> identities = user
                .getIdentities()
                .stream()
                .filter(i ->
                    SystemKeys.AUTHORITY_INTERNAL.equals(i.getAuthority()) && i.getProvider().equals(providerId)
                )
                .map(i -> (InternalUserIdentity) i)
                .collect(Collectors.toSet());

            // pick matching by username
            InternalUserIdentity identity = identities
                .stream()
                .filter(i -> i.getAccount().getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
            if (identity == null) {
                throw new IllegalArgumentException("error.invalid_user");
            }

            String userId = identity.getUserId();
            InternalUserAccount account = identity.getAccount();

            // fetch provider
            InternalIdentityService service = internalAuthority.getProvider(providerId);
            if (!service.getConfig().isEnableUpdate()) {
                throw new IllegalArgumentException("error.unsupported_operation");
            }

            // load realm props
            String realm = service.getRealm();
            model.addAttribute("realm", realm);
            model.addAttribute("displayName", realm);

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
                return "internal/changeaccount";
            }

            account = service.getAccountService().updateAccount(userId, username, account);
            model.addAttribute("account", account);

            return "internal/changeaccount_success";
        } catch (RegistrationException e) {
            model.addAttribute("error", e.getMessage());
            return "internal/changeaccount";
        } catch (Exception e) {
            model.addAttribute("error", RegistrationException.ERROR);
            return "internal/changeaccount";
        }
    }

    /**
     * Redirect to registration page
     * @throws RegistrationException
     */
    @Hidden
    @RequestMapping(value = "/auth/internal/register/{providerId}", method = RequestMethod.GET)
    public String registrationPage(@PathVariable("providerId") String providerId, Model model, Locale locale)
        throws NoSuchProviderException, NoSuchRealmException, RegistrationException {
        // resolve provider
        InternalIdentityService idp = internalAuthority.getProvider(providerId);
        if (!idp.getConfig().isEnableRegistration()) {
            throw new RegistrationException("unsupported_operation");
        }

        model.addAttribute("providerId", providerId);

        // load realm props
        String realm = idp.getRealm();
        Realm r = realmService.findRealm(realm);
        if (r == null) {
            throw new ProviderNotFoundException("realm not found");
        }

        model.addAttribute("realm", realm);
        model.addAttribute("displayName", realm);

        // build model
        model.addAttribute("reg", new UserRegistrationBean());

        //        // check if we have a clientId
        //        String clientId = (String) req.getSession().getAttribute(OAuth2Utils.CLIENT_ID);
        //        if (clientId != null) {
        //            // fetch client customizations for login screen
        //            Map<String, String> customizations = clientDetailsAdapter.getClientCustomizations(clientId);
        //            model.addAllAttributes(customizations);
        //        }

        // fetch password service if available
        AccountCredentialsService<?, ?, ?, ?> cs = idp.getCredentialsService(SystemKeys.AUTHORITY_PASSWORD);
        if (cs != null) {
            PasswordCredentialsService service = (PasswordCredentialsService) cs;
            // expose password policy by passing idp config
            PasswordPolicy policy = service.getPasswordPolicy();
            model.addAttribute("policy", policy);
        }

        // build url
        // TODO handle via urlBuilder or entryPoint
        model.addAttribute("registrationUrl", "/auth/internal/register/" + providerId);
        // set realm login url
        model.addAttribute("loginUrl", "/-/" + realm + "/login");

        if (r.getTosConfiguration() != null && r.getTosConfiguration().isEnableTOS()) {
            model.addAttribute("tosUrl", "/-/" + realm + "/terms");
        }

        return "internal/registeraccount";
    }

    /**
     * Register the user and redirect to the 'registersuccess' page
     */
    @Hidden
    @RequestMapping(value = "/auth/internal/register/{providerId}", method = RequestMethod.POST)
    public String register(
        Model model,
        Locale locale,
        @PathVariable("providerId") String providerId,
        @ModelAttribute("reg") @Valid UserRegistrationBean reg,
        BindingResult result,
        HttpServletRequest req
    ) {
        try {
            // resolve provider
            InternalIdentityService idp = internalAuthority.getProvider(providerId);
            if (!idp.getConfig().isEnableRegistration()) {
                throw new RegistrationException("unsupported_operation");
            }

            // build model for result
            model.addAttribute("providerId", providerId);

            // load realm props
            String realm = idp.getRealm();
            model.addAttribute("realm", realm);
            model.addAttribute("displayName", realm);

            // fetch password service if available
            AccountCredentialsService<?, ?, ?, ?> cs = idp.getCredentialsService(SystemKeys.AUTHORITY_PASSWORD);
            PasswordCredentialsService service = null;
            if (cs != null) {
                service = (PasswordCredentialsService) cs;
                // expose password policy by passing idp config
                PasswordPolicy policy = service.getPasswordPolicy();
                model.addAttribute("policy", policy);
            }

            model.addAttribute("registrationUrl", "/auth/internal/register/" + providerId);

            // set realm login url
            model.addAttribute("loginUrl", "/-/" + realm + "/login");

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

                return "internal/registeraccount";
            }

            String username = reg.getEmail();
            String password = reg.getPassword();
            String email = reg.getEmail();
            String name = reg.getName();
            String surname = reg.getSurname();
            String lang = reg.getLang();

            // validate password
            // TODO rework flow
            if (service != null && StringUtils.hasText(password)) {
                service.validatePassword(password);
            }

            // convert registration model to internal model
            InternalUserAccount account = new InternalUserAccount();
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
            InternalUserIdentity identity = new InternalUserIdentity(
                idp.getAuthority(),
                idp.getProvider(),
                idp.getRealm(),
                account
            );

            // register password
            if (service != null && StringUtils.hasText(password)) {
                InternalUserPassword pwd = new InternalUserPassword();
                pwd.setUserId(identity.getUserId());
                pwd.setUsername(username);
                pwd.setPassword(password);

                identity.setCredentials(Collections.singleton(pwd));
            }

            // register
            identity = idp.registerIdentity(subjectId, identity);

            model.addAttribute("account", account);

            if (idp.getConfig().isConfirmationRequired()) {
                return "internal/registeraccount_confirm";
            }

            return "internal/registeraccount_success";
        } catch (InvalidDataException | MissingDataException | DuplicatedDataException e) {
            StringBuilder msg = new StringBuilder();
            msg.append(messageSource.getMessage(e.getMessage(), null, req.getLocale()));
            msg.append(": ");
            msg.append(messageSource.getMessage("field." + e.getField(), null, req.getLocale()));

            model.addAttribute("error", msg.toString());
            return "internal/registeraccount";
        } catch (RegistrationException e) {
            model.addAttribute("error", e.getMessage());
            return "internal/registeraccount";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            model.addAttribute("error", RegistrationException.ERROR);
            return "internal/registeraccount";
        }
    }
}
