package it.smartcommunitylab.aac.internal.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidPasswordException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.dto.CustomizationBean;
import it.smartcommunitylab.aac.dto.UserEmailBean;
import it.smartcommunitylab.aac.dto.UserPasswordBean;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;
import it.smartcommunitylab.aac.internal.dto.PasswordPolicy;
import it.smartcommunitylab.aac.internal.model.UserPasswordCredentials;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityService;
import it.smartcommunitylab.aac.internal.provider.InternalPasswordService;
import it.smartcommunitylab.aac.model.Realm;

@Controller
@RequestMapping
public class InternalCredentialsController {

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private InternalIdentityAuthority internalAuthority;

    @Autowired
    private RealmManager realmManager;

    /*
     * Password change
     */

    @GetMapping("/changepwd/{providerId}/{uuid}")
    public String changepwd(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid,
            Model model)
            throws NoSuchProviderException, NoSuchUserException {
        // first check userid vs user
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }

        // fetch internal identities matching provider
        Set<UserIdentity> identities = user.getIdentities().stream().filter(
                i -> SystemKeys.AUTHORITY_INTERNAL.equals(i.getAuthority()) && i.getProvider().equals(providerId))
                .collect(Collectors.toSet());

        // pick matching by uuid
        UserIdentity identity = identities.stream().filter(i -> i.getAccount().getUuid().equals(uuid))
                .findFirst().orElse(null);
        if (identity == null) {
            throw new IllegalArgumentException("error.invalid_user");
        }

        String userId = identity.getUserId();
        UserAccount account = identity.getAccount();

        // fetch provider
        InternalIdentityService idp = internalAuthority.getIdentityService(providerId);

        // fetch credentials service if available
        InternalPasswordService service = idp.getCredentialsService();

        if (service == null) {
            throw new IllegalArgumentException("error.unsupported_operation");
        }

        if (!service.canSet()) {
            throw new IllegalArgumentException("error.unsupported_operation");
        }

        // for internal username is accountId
        String username = account.getAccountId();
        UserPasswordCredentials cred = service.getCredentials(username);
        UserPasswordBean reg = new UserPasswordBean();
        reg.setUsername(username);
//        reg.setPassword("");
//        reg.setVerifyPassword(null);

        // expose password policy by passing idp config
        PasswordPolicy policy = service.getPasswordPolicy();

        // build model
        model.addAttribute("userId", userId);
        model.addAttribute("username", account.getUsername());
        model.addAttribute("uuid", account.getUuid());
        model.addAttribute("credentials", cred);
        model.addAttribute("reg", reg);
        model.addAttribute("policy", policy);
        model.addAttribute("accountUrl", "/account");
        model.addAttribute("changeUrl", "/changepwd/" + providerId + "/" + uuid);
        return "registration/changepwd";
    }

    @PostMapping("/changepwd/{providerId}/{uuid}")
    public String changepwd(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid,
            Model model,
            @ModelAttribute("reg") @Valid UserPasswordBean reg,
            BindingResult result)
            throws NoSuchProviderException, NoSuchUserException {

        try {
            // first check userid vs user
            UserDetails user = authHelper.getUserDetails();
            if (user == null) {
                throw new InsufficientAuthenticationException("error.unauthenticated_user");
            }

            // fetch internal identities matching provider
            Set<UserIdentity> identities = user.getIdentities().stream().filter(
                    i -> SystemKeys.AUTHORITY_INTERNAL.equals(i.getAuthority()) && i.getProvider().equals(providerId))
                    .collect(Collectors.toSet());

            // pick matching by username
            UserIdentity identity = identities.stream().filter(i -> i.getAccount().getUuid().equals(uuid))
                    .findFirst().orElse(null);
            if (identity == null) {
                throw new IllegalArgumentException("error.invalid_user");
            }

            String userId = identity.getUserId();
            UserAccount account = identity.getAccount();

            // fetch provider
            InternalIdentityService idp = internalAuthority.getIdentityService(providerId);
            String realm = idp.getRealm();

            // fetch credentials service if available
            InternalPasswordService service = idp.getCredentialsService();

            if (service == null) {
                throw new IllegalArgumentException("error.unsupported_operation");
            }

            if (!service.canSet()) {
                throw new IllegalArgumentException("error.unsupported_operation");
            }

            // for internal username is accountId
            String username = account.getAccountId();

            // get current password
            UserPasswordCredentials cur = service.getCredentials(username);
            model.addAttribute("userId", userId);
            model.addAttribute("username", username);
            model.addAttribute("uuid", account.getUuid());
            model.addAttribute("credentials", cur);

            // expose password policy by passing idp config
            PasswordPolicy policy = service.getPasswordPolicy();
            model.addAttribute("policy", policy);

            model.addAttribute("accountUrl", "/account");
            model.addAttribute("changeUrl", "/changepwd/" + providerId + "/" + uuid);

            if (result.hasErrors()) {
                return "registration/changepwd";
            }

            // check curPassword match
            String curPassword = reg.getCurPassword();
            if (!service.verifyPassword(username, curPassword)) {
                throw new RegistrationException("error.wrong_password");
            }

            String password = reg.getPassword();
            String verifyPassword = reg.getVerifyPassword();

            if (!password.equals(verifyPassword)) {
                // error
                throw new RegistrationException("error.mismatch_passwords");
            }

//            // if cur has changeOnFirstAccess we skip verification
//            if (!cur.isChangeOnFirstAccess()) {
//                boolean valid = service.verifyPassword(userId, reg.getCurPassword());
//                if (!valid) {
//                    throw new RegistrationException("invalid verification password");
//                }
//            }

            // update
            UserPasswordCredentials pwd = new UserPasswordCredentials(SystemKeys.AUTHORITY_INTERNAL, providerId, realm,
                    userId);
            pwd.setPassword(password);
            pwd = service.setCredentials(username, pwd);

            return "registration/changesuccess";
        } catch (InvalidPasswordException e) {
            String msg = e.getError() + "." + e.getMessage();
            model.addAttribute("error", msg);
            return "registration/changepwd";
        } catch (RegistrationException e) {
            String msg = e.getMessage();
            model.addAttribute("error", msg);
            return "registration/changepwd";
        } catch (Exception e) {
            model.addAttribute("error", RegistrationException.class.getSimpleName());
            return "registration/changepwd";
        }
    }

    /*
     * Password reset
     */

    @GetMapping("/auth/internal/reset/{providerId}")
    public String resetPage(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            Model model) throws NoSuchProviderException, NoSuchRealmException {

        // resolve provider
        InternalIdentityService idp = internalAuthority.getIdentityService(providerId);

        if (!idp.getCredentialsService().canReset()) {
            throw new RegistrationException("error.unsupported_operation");
        }

        model.addAttribute("providerId", providerId);

        String realm = idp.getRealm();
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
        model.addAttribute("reg", new UserEmailBean());

        // build url
        // TODO handle via urlBuilder or entryPoint
        model.addAttribute("resetUrl", "/auth/internal/reset/" + providerId);
        model.addAttribute("loginUrl", "/-/" + realm + "/login");

        return "registration/resetpwd";
    }

    @PostMapping("/auth/internal/reset/{providerId}")
    public String reset(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            Model model,
            @ModelAttribute("reg") @Valid UserEmailBean reg,
            BindingResult result) {

        try {

            // resolve provider
            InternalIdentityService idp = internalAuthority.getIdentityService(providerId);
            if (!idp.getCredentialsService().canReset()) {
                throw new RegistrationException("reset is disabled");
            }

            String realm = idp.getRealm();

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

            // reset is available only by email
            String email = reg.getEmail();
            if (!StringUtils.hasText(email)) {
                result.rejectValue("email", "error.invalid_email");
            }

            if (result.hasErrors()) {
                return "registration/resetpwd";
            }

            // resolve user
            InternalUserAccount account = idp.getAccountProvider().findAccountByEmail(email);
            if (account == null) {
                // don't leak error
                result.rejectValue("email", "error.invalid_email");
                return "registration/resetpwd";

            } else {
                // direct call to reset
                InternalPasswordService passwordService = idp.getCredentialsService();
                account = passwordService.resetPassword(account.getUsername());

                model.addAttribute("reg", reg);

                return "registration/resetsuccess";
            }
        } catch (RegistrationException e) {
            model.addAttribute("error", e.getError());
            return "registration/resetpwd";
        } catch (Exception e) {
            model.addAttribute("error", RegistrationException.ERROR);
            return "registration/resetpwd";
        }
    }

}
