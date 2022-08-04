package it.smartcommunitylab.aac.webauthn.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorTransport;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
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
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAuthenticationException;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnAttestationResponse;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnCredentialCreationInfo;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationResponse;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationStartRequest;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProvider;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnCredentialsService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnRpService;

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
    @RequestMapping(value = "/webauthn/register/{providerId}/{uuid}", method = RequestMethod.GET)
    public String credentialsRegistrationPage(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid,
            Model model) throws NoSuchProviderException, NoSuchRealmException {
        // first check uuid vs user
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
        WebAuthnIdentityProvider idp = webAuthnAuthority.getProvider(providerId);

        // fetch credentials service if available
        WebAuthnCredentialsService service = idp.getCredentialsService();

        if (service == null) {
            throw new IllegalArgumentException("error.unsupported_operation");
        }

        if (!idp.getConfig().isEnableRegistration()) {
            throw new IllegalArgumentException("error.unsupported_operation");
        }

        // for internal username is accountId
        String username = account.getAccountId();

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

        // build model
        WebAuthnRegistrationStartRequest bean = new WebAuthnRegistrationStartRequest();
        bean.setUsername(username);
        bean.setProvider(providerId);
        bean.setUserHandle(uuid);
        model.addAttribute("reg", bean);

        // build url
        // TODO handle via urlBuilder or entryPoint
        model.addAttribute("registrationUrl", "/auth/webauthn/credentials/" + providerId + "/" + uuid);
        model.addAttribute("loginUrl", "/-/" + realm + "/login");

        // return credentials registration page
        return "webauthn/register";
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

        // first check uuid vs user
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }

        // fetch internal identities
        Set<UserIdentity> identities = user.getIdentities().stream()
                .filter(i -> (i instanceof InternalUserIdentity))
                .collect(Collectors.toSet());

        // pick matching by uuid == userhandle
        String uuid = reg.getUserHandle();
        UserIdentity identity = identities.stream().filter(i -> i.getAccount().getUuid().equals(uuid))
                .findFirst().orElse(null);
        if (identity == null) {
            throw new IllegalArgumentException("error.invalid_user");
        }

        String userId = identity.getUserId();
        UserAccount account = identity.getAccount();

        // fetch provider
        WebAuthnIdentityProvider idp = webAuthnAuthority.getProvider(providerId);

        // fetch credentials service if available
        WebAuthnCredentialsService service = idp.getCredentialsService();

        if (service == null) {
            throw new IllegalArgumentException("error.unsupported_operation");
        }

        if (!idp.getConfig().isEnableRegistration()) {
            throw new IllegalArgumentException("error.unsupported_operation");
        }

        // for internal username is accountId
        String username = account.getAccountId();
        if (!username.equals(reg.getUsername())) {
            throw new IllegalArgumentException("error.invalid_user");
        }

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
     * @throws NoSuchUserException
     * @throws RegistrationException
     * @throws WebAuthnAuthenticationException
     */
    @Hidden
    @PostMapping(value = "/auth/webauthn/attestations/{providerId}/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String verifyAttestation(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid,
            @RequestBody @Valid WebAuthnAttestationResponse body) throws NoSuchProviderException,
            WebAuthnAuthenticationException, RegistrationException, NoSuchUserException {

        // first check uuid vs user
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
        WebAuthnIdentityProvider idp = webAuthnAuthority.getProvider(providerId);

        // fetch credentials service if available
        WebAuthnCredentialsService service = idp.getCredentialsService();

        if (service == null) {
            throw new IllegalArgumentException("error.unsupported_operation");
        }

        if (!idp.getConfig().isEnableRegistration()) {
            throw new IllegalArgumentException("error.unsupported_operation");
        }

        // for internal username is accountId
        String username = account.getAccountId();

        // fetch registration
        String key = body.getKey();
        WebAuthnCredentialCreationInfo info = rpService.getRegistration(key);
        if (info == null) {
            // no registration in progress with this key
            throw new IllegalArgumentException();
        }

        if (!username.equals(info.getUsername())) {
            // not for this user
            throw new IllegalArgumentException("error.invalid_user");
        }

        try {
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc = PublicKeyCredential
                    .parseRegistrationResponseJson(body.toJson());

            RegistrationResult result = rpService.finishRegistration(providerId, key, pkc);

            String userHandle = account.getUuid();

            // create a new credential in repository for the result
            WebAuthnCredential credential = new WebAuthnCredential();
            credential.setUsername(username);
            credential.setCredentialId(result.getKeyId().getId().getBase64());
            credential.setUserHandle(userHandle);

            credential.setDisplayName(info.getDisplayName());
            credential.setPublicKeyCose(result.getPublicKeyCose().getBase64());
            credential.setSignatureCount(result.getSignatureCount());

            if (result.getKeyId().getTransports().isPresent()) {
                Set<AuthenticatorTransport> transports = result.getKeyId().getTransports().get();
                List<String> transportCodes = transports.stream().map(t -> t.getId()).collect(Collectors.toList());
                credential.setTransports(StringUtils.collectionToCommaDelimitedString(transportCodes));
            }

            Boolean discoverable = result.isDiscoverable().isPresent() ? result.isDiscoverable().get() : null;
            credential.setDiscoverable(discoverable);

            // TODO add support for additional fields in registration

            // register as new
            credential = service.setCredentials(username, credential);

            return credential.getId();

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid attestation");
        }
    }

}
