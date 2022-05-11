package it.smartcommunitylab.aac.webauthn.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.dto.CustomizationBean;
import it.smartcommunitylab.aac.dto.UserRegistrationBean;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityService;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnAttestationResponse;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationResponse;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationStartRequest;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnRpService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnRpRegistrationRepository;

/**
 * Manages the endpoints connected to the registration ceremony of WebAuthn.
 * 
 * The registration ceremony is used to register new WebAuthn credentials on
 * this server.
 */
@Controller
@RequestMapping
public class WebAuthnRegistrationController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private WebAuthnIdentityAuthority webAuthnAuthority;

    @Autowired
    private RealmManager realmManager;

    @Autowired
    private WebAuthnRpService rpService;

    @Hidden
    @RequestMapping(value = "/auth/webauthn/register/{providerId}", method = RequestMethod.GET)
    public String userRegistrationPage(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            Model model) throws NoSuchProviderException, NoSuchRealmException {
        // resolve provider
        WebAuthnIdentityService idp = webAuthnAuthority.getIdentityService(providerId);
        if (idp == null) {
            throw new NoSuchProviderException();
        }

        if (!idp.getConfig().isEnableRegistration()) {
            throw new IllegalArgumentException();
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

        // build url
        // TODO handle via urlBuilder or entryPoint
        model.addAttribute("registrationUrl", "/auth/webauthn/register/" + providerId);
        model.addAttribute("loginUrl", "/-/" + realm + "/login");

        return "webauthn/register";
    }

    /**
     * Register the user and redirect to the 'registersuccess' page
     */
    @Hidden
    @RequestMapping(value = "/auth/webauthn/register/{providerId}", method = RequestMethod.POST)
    public String registerUser(Model model,
            @PathVariable("providerId") String providerId,
            @ModelAttribute("reg") @Valid UserRegistrationBean reg,
            BindingResult result,
            HttpServletRequest req) {
        try {
            // resolve provider
            WebAuthnIdentityService idp = webAuthnAuthority.getIdentityService(providerId);
            if (idp == null) {
                throw new NoSuchProviderException();
            }

            if (!idp.getConfig().isEnableRegistration()) {
                throw new IllegalArgumentException();
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

            model.addAttribute("registrationUrl", "/auth/webauthn/register/" + providerId);
            model.addAttribute("loginUrl", "/-/" + realm + "/login");

            if (result.hasErrors()) {
                model.addAttribute("error", InvalidDataException.ERROR);
                return "webauthn/register";
            }

            String username = reg.getEmail();
            String email = reg.getEmail();
            String name = reg.getName();
            String surname = reg.getSurname();
            String lang = reg.getLang();

            // convert registration model to internal model
            WebAuthnUserAccount account = new WebAuthnUserAccount();
            account.setUsername(username);
            account.setEmailAddress(email);
            account.setName(name);
            account.setSurname(surname);
            account.setLang(lang);

            // TODO handle additional attributes from registration

            // TODO handle subject resolution to link with existing accounts
            // either via current session or via providers from same realm
            // TODO force require confirmation if linking
            String subjectId = null;

            // register
            UserIdentity identity = idp.registerIdentity(subjectId, account, Collections.emptyList());
            model.addAttribute("identity", identity);

            // check if we require confirmation before adding credentials
            if (idp.getConfig().isEnableConfirmation()) {
                // return success page and wait for callback via link
                return "registration/regsuccess";
            } else {
                // build a session
                // TODO rework via magic link (passwordless) + filter
                
//                
//                // move to credentials registration with this account
//                buildCredentialsRegistration(model, providerId, account);
//                model.addAttribute("registrationUrl", "/auth/webauthn/credentials/" + providerId);
//
//                // return credentials registration page
//                return "webauthn/credential";
                return "registration/regsuccess";

            }

        } catch (RegistrationException e) {
            String error = e.getError();
            model.addAttribute("error", error);
            return "webauthn/register";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            model.addAttribute("error", RegistrationException.ERROR);
            return "webauthn/register";
        }
    }

    private void buildCredentialsRegistration(
            Model model,
            String providerId,
            WebAuthnUserAccount account) throws NoSuchProviderException, NoSuchRealmException {

        // build model
        WebAuthnRegistrationStartRequest bean = new WebAuthnRegistrationStartRequest();
        bean.setUsername(account.getUsername());
        model.addAttribute("reg", bean);

    }

    @Hidden
    @RequestMapping(value = "/auth/webauthn/credentials/{providerId}/{userHandle}", method = RequestMethod.GET)
    public String credentialsRegistrationPage(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userHandle,
            Model model) throws NoSuchProviderException, NoSuchRealmException {

        // first check user
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("User must be authenticated");
        }

        // resolve provider
        WebAuthnIdentityService idp = webAuthnAuthority.getIdentityService(providerId);
        if (idp == null) {
            throw new NoSuchProviderException();
        }

        if (!idp.getConfig().isEnableRegistration()) {
            throw new IllegalArgumentException();
        }

        // resolve account via userHandle if currently logged
        Set<WebAuthnUserAccount> accounts = user.getIdentities().stream()
                .filter(i -> SystemKeys.AUTHORITY_WEBAUTHN.equals(i.getAuthority())
                        && i.getProvider().equals(providerId))
                .map(i -> (WebAuthnUserAccount) i.getAccount())
                .collect(Collectors.toSet());

        // pick matching
        WebAuthnUserAccount account = accounts.stream()
                .filter(a -> a.getUserHandle().equals(userHandle))
                .findFirst().orElse(null);

        if (account == null) {
            throw new IllegalArgumentException("userhandle invalid");
        }

        // build model for this account
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

        buildCredentialsRegistration(model, providerId, account);

        // build url
        // TODO handle via urlBuilder or entryPoint
        model.addAttribute("registrationUrl", "/auth/webauthn/credentials/" + providerId);
        model.addAttribute("loginUrl", "/-/" + realm + "/login");

        // return credentials registration page
        return "webauthn/credential";
    }

    /**
     * Starts a new WebAuthn registration ceremony by generating a new Credential
     * Creation Options object and returning it to the user.
     * 
     * The challenge and the information about the ceremony are temporarily stored
     * in the session.
     * 
     * @throws NoSuchProviderException
     * @throws NoSuchUserException
     * @throws RegistrationException
     */
    @Hidden
    @PostMapping(value = "/auth/webauthn/attestationOptions/{providerId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public WebAuthnRegistrationResponse generateAttestationOptions(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @RequestBody @Valid WebAuthnRegistrationStartRequest reg)
            throws NoSuchProviderException, RegistrationException, NoSuchUserException {
        // resolve provider
        WebAuthnIdentityService idp = webAuthnAuthority.getIdentityService(providerId);
        if (idp == null) {
            throw new NoSuchProviderException();
        }

        if (!idp.getConfig().isEnableRegistration()) {
            throw new IllegalArgumentException();
        }

        String username = reg.getUsername();
        String displayName = reg.getDisplayName();

        WebAuthnRegistrationResponse response = rpService.startRegistration(
                providerId, username, displayName);
        return response;
    }

    /**
     * Validates the attestation generated using the Credential Creation Options
     * obtained through the {@link #generateAttestationOptions} controller
     * 
     * @throws NoSuchProviderException
     */
    @Hidden
    @PostMapping(value = "/auth/webauthn/attestations/{providerId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String verifyAttestation(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @RequestBody @Valid WebAuthnAttestationResponse body) throws NoSuchProviderException {
        try {
            final boolean canRegister = registrationRepository.findByProviderId(providerId)
                    .getConfigMap()
                    .isEnableRegistration();
            if (!canRegister) {
                throw new RegistrationException("registration is disabled");
            }
            WebAuthnRpService rps = webAuthnRpServiceRegistrationRepository.get(providerId);

            String key = body.getKey();

            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc = PublicKeyCredential
                    .parseRegistrationResponseJson(body.toJson());

            final String authenticatedUser = rps.finishRegistration(pkc, providerId, key);
            if (!StringUtils.hasText(authenticatedUser)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid attestation");
            }
            return "Welcome " + authenticatedUser + ". Next step is to authenticate your session";
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid attestation");
        }
    }

}
