package it.smartcommunitylab.aac.internal.controller;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.dto.UserPasswordBean;
import it.smartcommunitylab.aac.internal.dto.PasswordPolicy;
import it.smartcommunitylab.aac.internal.model.UserPasswordCredentials;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProvider;
import it.smartcommunitylab.aac.internal.provider.InternalPasswordService;

@Controller
@RequestMapping
public class CredentialsController {

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private ProviderManager providerManager;

    @GetMapping("/changepwd/{providerId}/{username}")
    public String changepwd(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String username,
            Model model)
            throws NoSuchProviderException, NoSuchUserException {
        // first check userid vs user
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("User must be authenticated");
        }

        // export id
        String userId = SystemKeys.AUTHORITY_INTERNAL + "|" + providerId + "|" + username;
        UserIdentity identity = user.getIdentity(userId);
        if (identity == null) {
            throw new IllegalArgumentException("userid invalid");
        }

        UserAccount account = identity.getAccount();

        // check if internal authority
        if (!SystemKeys.AUTHORITY_INTERNAL.equals(account.getAuthority())
                || !account.getProvider().equals(providerId)) {
            throw new IllegalArgumentException("account invalid");
        }

        // fetch provider
        IdentityService idp = providerManager.getIdentityService(providerId);

        if (!(idp instanceof InternalIdentityProvider)) {
            throw new IllegalArgumentException("service invalid");
        }

        // fetch credentials service if available
        InternalPasswordService service = ((InternalIdentityProvider) idp).getCredentialsService();

        if (service == null) {
            throw new IllegalArgumentException("credentials are immutable");
        }

        if (!service.canSet()) {
            throw new IllegalArgumentException("credentials are immutable");
        }

        UserPasswordCredentials cred = service.getUserCredentials(userId);
        UserPasswordBean reg = new UserPasswordBean();
        reg.setUserId(userId);
        reg.setPassword("");
        reg.setVerifyPassword(null);

        // expose password policy by passing idp config
        PasswordPolicy policy = service.getPasswordPolicy();

        // build model
        model.addAttribute("username", username);
        model.addAttribute("credentials", cred);
        model.addAttribute("reg", reg);
        model.addAttribute("policy", policy);
        model.addAttribute("accountUrl", "/account");
        model.addAttribute("changeUrl", "/changepwd/" + providerId+"/"+username);
        return "registration/changepwd";
    }

    @PostMapping("/changepwd/{providerId}/{username}")
    public String changepwd(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String username,
            Model model,
            @ModelAttribute("reg") @Valid UserPasswordBean reg,
            BindingResult result)
            throws NoSuchProviderException, NoSuchUserException {

        try {
            // unpack
            String userId = reg.getUserId();

            // first check userid vs user
            UserDetails user = authHelper.getUserDetails();
            if (user == null) {
                throw new InsufficientAuthenticationException("User must be authenticated");
            }

            UserIdentity identity = user.getIdentity(userId);
            if (identity == null) {
                throw new IllegalArgumentException("userid invalid");
            }

            UserAccount account = identity.getAccount();

            // check if internal authority
            if (!SystemKeys.AUTHORITY_INTERNAL.equals(account.getAuthority())
                    || !account.getProvider().equals(providerId)) {
                throw new IllegalArgumentException("account invalid");
            }

            // fetch provider
            IdentityService idp = providerManager.getIdentityService(providerId);

            if (!(idp instanceof InternalIdentityProvider)) {
                throw new IllegalArgumentException("service invalid");
            }

            // fetch credentials service if available
            InternalPasswordService service = ((InternalIdentityProvider) idp).getCredentialsService();

            if (service == null) {
                throw new IllegalArgumentException("credentials are immutable");
            }

            if (!service.canSet()) {
                throw new IllegalArgumentException("credentials are immutable");
            }

            // get current password
            UserPasswordCredentials cur = service.getUserCredentials(userId);
            model.addAttribute("username", account.getUsername());
            model.addAttribute("credentials", cur);

            // expose password policy by passing idp config
            PasswordPolicy policy = service.getPasswordPolicy();
            model.addAttribute("policy", policy);

            model.addAttribute("accountUrl", "/account");
            model.addAttribute("changeUrl", "/changepwd/" + providerId+"/"+username);

            if (result.hasErrors()) {
                return "registration/changepwd";
            }

            String password = reg.getPassword();
            String verifyPassword = reg.getVerifyPassword();

            if (!password.equals(verifyPassword)) {
                // error
                throw new RegistrationException("passwords do not match");
            }

            // if cur has changeOnFirstAccess we skip verification
            if (!cur.isChangeOnFirstAccess()) {
                boolean valid = service.verifyPassword(userId, reg.getCurPassword());
                if (!valid) {
                    throw new RegistrationException("invalid verification password");
                }
            }

            // update
            UserPasswordCredentials pwd = new UserPasswordCredentials();
            pwd.setUserId(userId);
            pwd.setPassword(password);
            pwd = service.setUserCredentials(userId, pwd);

            return "registration/changesuccess";
        } catch (RegistrationException e) {
            model.addAttribute("error", e.getMessage());
            return "registration/changepwd";
        } catch (Exception e) {
            model.addAttribute("error", RegistrationException.class.getSimpleName());
            return "registration/changepwd";
        }
    }

}
