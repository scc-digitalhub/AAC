package it.smartcommunitylab.aac.password.controller;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
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
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.dto.UserEmail;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.password.PasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.PasswordCredentialsAuthority;
import it.smartcommunitylab.aac.password.dto.UserPasswordBean;
import it.smartcommunitylab.aac.password.model.PasswordPolicy;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProvider;
import it.smartcommunitylab.aac.password.provider.PasswordCredentialsService;

@Controller
@RequestMapping
public class PasswordCredentialsController {

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private PasswordCredentialsAuthority passwordAuthority;

    @Autowired
    private PasswordIdentityAuthority identityAuthority;

    /*
     * Password change
     * 
     * via credentials service
     */

    @GetMapping("/changepwd/{providerId}/{uuid}")
    public String changepwd(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid,
            HttpServletRequest request, Model model, Locale locale)
            throws NoSuchProviderException, NoSuchUserException, NoSuchRealmException {

        // first check userid vs user
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }

        // fetch internal identities
        Set<UserIdentity> identities = user.getIdentities().stream()
                .filter(i -> (i instanceof InternalUserIdentity))
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
        PasswordCredentialsService service = passwordAuthority.getProvider(providerId);
        if (!service.canSet()) {
            throw new IllegalArgumentException("error.unsupported_operation");
        }

        // for internal username is accountId
        String username = account.getAccountId();
        InternalUserPassword cred = service.getCredentials(username);
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

        String code = (String) request.getSession().getAttribute("resetCode");
        if (code != null) {
            model.addAttribute("resetCode", code);
        }

        // load realm props
        String realm = user.getRealm();
        model.addAttribute("realm", realm);
        model.addAttribute("displayName", realm);

        return "password/changepwd";
    }

    @PostMapping("/changepwd/{providerId}/{uuid}")
    public String changepwd(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid,
            Model model, Locale locale,
            @ModelAttribute("reg") @Valid UserPasswordBean reg,
            HttpServletRequest request, BindingResult result)
            throws NoSuchProviderException, NoSuchUserException {

        try {
            // first check userid vs user
            UserDetails user = authHelper.getUserDetails();
            if (user == null) {
                throw new InsufficientAuthenticationException("error.unauthenticated_user");
            }

            // fetch internal identities
            Set<UserIdentity> identities = user.getIdentities().stream()
                    .filter(i -> (i instanceof InternalUserIdentity))
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
            PasswordCredentialsService service = passwordAuthority.getProvider(providerId);
            if (!service.canSet()) {
                throw new IllegalArgumentException("error.unsupported_operation");
            }

            // for internal username is accountId
            String username = account.getAccountId();

            // get current password
            InternalUserPassword cur = service.getCredentials(username);
            model.addAttribute("userId", userId);
            model.addAttribute("username", username);
            model.addAttribute("uuid", account.getUuid());
            model.addAttribute("credentials", cur);

            // expose password policy by passing idp config
            PasswordPolicy policy = service.getPasswordPolicy();
            model.addAttribute("policy", policy);

            String code = (String) request.getSession().getAttribute("resetCode");
            if (code != null) {
                model.addAttribute("resetCode", code);
            }

            model.addAttribute("accountUrl", "/account");
            model.addAttribute("changeUrl", "/changepwd/" + providerId + "/" + uuid);

            // load realm props
            String realm = user.getRealm();
            model.addAttribute("realm", realm);
            model.addAttribute("displayName", realm);

            if (result.hasErrors()) {
                return "password/changepwd";
            }

            if (request.getSession().getAttribute("resetCode") == null) {
                // check curPassword match
                String curPassword = reg.getCurPassword();
                if (!service.verifyPassword(username, curPassword)) {
                    throw new RegistrationException("wrong_password");
                }
            }

            String password = reg.getPassword();
            String verifyPassword = reg.getVerifyPassword();

            if (!password.equals(verifyPassword)) {
                // error
                throw new RegistrationException("mismatch_passwords");
            }

//            // if cur has changeOnFirstAccess we skip verification
//            if (!cur.isChangeOnFirstAccess()) {
//                boolean valid = service.verifyPassword(userId, reg.getCurPassword());
//                if (!valid) {
//                    throw new RegistrationException("invalid verification password");
//                }
//            }

            // update
            InternalUserPassword pwd = new InternalUserPassword();
            pwd.setUserId(userId);
            pwd.setUsername(username);
            pwd.setPassword(password);
            pwd = service.setCredentials(username, pwd);

            request.getSession().removeAttribute("resetCode");

            return "password/changepwd_success";
        } catch (RegistrationException e) {
            model.addAttribute("error", e.getMessage());
            return "password/changepwd";
        } catch (Exception e) {
            model.addAttribute("error", RegistrationException.ERROR);
            return "password/changepwd";
        }
    }

    /*
     * Password reset
     * 
     * via idp
     */

    @GetMapping("/auth/password/reset/{providerId}")
    public String resetPage(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            Model model, Locale locale) throws NoSuchProviderException, NoSuchRealmException {

        // fetch provider
        PasswordIdentityProvider idp = identityAuthority.getProvider(providerId);
        if (!idp.getConfig().isEnablePasswordReset()) {
            throw new IllegalArgumentException("error.unsupported_operation");
        }

        model.addAttribute("providerId", providerId);

        // load realm props
        String realm = idp.getRealm();
        model.addAttribute("realm", realm);
        model.addAttribute("displayName", realm);

        // build model
        model.addAttribute("reg", new UserEmail());

        // build url
        // TODO handle via urlBuilder or entryPoint
        model.addAttribute("resetUrl", "/auth/password/reset/" + providerId);
        // user realm login
        model.addAttribute("loginUrl", "/-/" + realm + "/login");

        return "password/resetpwd";
    }

    @PostMapping("/auth/password/reset/{providerId}")
    public String reset(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            Model model, Locale locale,
            @ModelAttribute("reg") @Valid UserEmail reg, BindingResult result) {

        try {
            // fetch provider
            PasswordIdentityProvider idp = identityAuthority.getProvider(providerId);
            if (!idp.getConfig().isEnablePasswordReset()) {
                throw new IllegalArgumentException("error.unsupported_operation");
            }
            // fetch provider
            PasswordCredentialsService service = passwordAuthority.getProvider(providerId);
            if (!service.canSet()) {
                throw new IllegalArgumentException("error.unsupported_operation");
            }

            // build model for result
            model.addAttribute("providerId", providerId);

            // load realm props
            String realm = idp.getRealm();
            model.addAttribute("realm", realm);
            model.addAttribute("displayName", realm);

            model.addAttribute("resetUrl", "/auth/password/reset/" + providerId);
            // user realm login
            model.addAttribute("loginUrl", "/-/" + realm + "/login");

            // reset is available only by email
            String email = reg.getEmail();
            if (!StringUtils.hasText(email)) {
                result.rejectValue("email", "error.invalid_email");
            }

            if (result.hasErrors()) {
                return "password/resetpwd";
            }

            // resolve username
            InternalUserAccount account = idp.getAccountProvider().findAccountByEmail(email);
            if (account == null) {
                // don't leak error
//                result.rejectValue("email", "error.invalid_email");
//                return "password/resetpwd";
                throw new RegistrationException("invalid_email");
            }

            String username = account.getUsername();

            // direct call to reset
            // will also send mail
            service.resetCredentials(username);

            model.addAttribute("reg", reg);

            // set idp form as login url on success
            model.addAttribute("loginUrl", "/auth/password/form/" + providerId);

            return "password/resetpwd_success";
        } catch (RegistrationException e) {
            model.addAttribute("error", e.getMessage());
            return "password/resetpwd";
        } catch (Exception e) {
            model.addAttribute("error", RegistrationException.ERROR);
            return "password/resetpwd";
        }
    }

}
