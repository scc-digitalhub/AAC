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

import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.identity.model.UserIdentity;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.realms.RealmManager;
import it.smartcommunitylab.aac.webauthn.WebAuthnCredentialsAuthority;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAuthenticationException;
import it.smartcommunitylab.aac.webauthn.model.AttestationResponse;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationRequest;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationResponse;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationStartRequest;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnCredentialsService;
import it.smartcommunitylab.aac.webauthn.store.WebAuthnRegistrationRequestStore;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private WebAuthnCredentialsAuthority webAuthnAuthority;

    @Autowired
    private RealmManager realmManager;

    @Autowired
    private WebAuthnRegistrationRequestStore requestStore;

    @Hidden
    @RequestMapping(value = "/webauthn/register/{providerId}/{uuid}", method = RequestMethod.GET)
    public String credentialsRegistrationPage(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid,
        Model model
    ) throws NoSuchProviderException, NoSuchRealmException {
        // first check uuid vs user
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }

        // fetch internal identities
        Set<UserIdentity> identities = user
            .getIdentities()
            .stream()
            .filter(i -> (i instanceof InternalUserIdentity))
            .collect(Collectors.toSet());

        // pick matching by uuid
        UserIdentity identity = identities
            .stream()
            .filter(i -> i.getAccount().getUuid().equals(uuid))
            .findFirst()
            .orElse(null);
        if (identity == null) {
            throw new IllegalArgumentException("error.invalid_user");
        }

        UserAccount account = identity.getAccount();

        // fetch provider
        WebAuthnCredentialsService service = webAuthnAuthority.getProvider(providerId);

        logger.debug(
            "register credentials for {} with provider {}",
            StringUtils.trimAllWhitespace(uuid),
            StringUtils.trimAllWhitespace(providerId)
        );

        // for internal username is accountId
        String username = account.getAccountId();

        // build model for this account
        model.addAttribute("providerId", providerId);

        String realm = service.getRealm();
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
        //            CustomizationBean rcb = re.getCustomization("registration");
        //            if (rcb != null) {
        //                resources.putAll(rcb.getResources());
        //            }
        //        }

        model.addAttribute("displayName", displayName);
        model.addAttribute("customization", resources);

        // build model
        WebAuthnRegistrationStartRequest bean = new WebAuthnRegistrationStartRequest();
        bean.setUsername(username);
        model.addAttribute("reg", bean);

        // build url
        // TODO handle via urlBuilder or entryPoint
        model.addAttribute("registrationUrl", "/webauthn/attestations/" + providerId);
        model.addAttribute("loginUrl", "/-/" + realm + "/login");
        model.addAttribute("accountUrl", "/account");

        // return credentials registration page
        return "webauthn/registercredentials";
    }

    /**
     * Starts a new WebAuthn registration ceremony by generating a new Credential
     * Creation Options object and returning it to the user.
     *
     * The challenge and the information about the ceremony are temporarily stored
     * in the session.
     */
    @Hidden
    @PostMapping(
        value = "/auth/webauthn/attestationOptions/{providerId}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public WebAuthnRegistrationResponse generateAttestationOptions(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        @RequestBody @Valid WebAuthnRegistrationStartRequest reg
    ) throws NoSuchProviderException, RegistrationException, NoSuchUserException {
        // first check uuid vs user
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }

        // fetch internal identities
        Set<UserIdentity> identities = user
            .getIdentities()
            .stream()
            .filter(i -> (i instanceof InternalUserIdentity))
            .collect(Collectors.toSet());

        // pick matching by username and ignore provider
        String username = reg.getUsername();
        UserIdentity identity = identities
            .stream()
            .filter(i -> i.getAccount().getUsername().equals(username))
            .findFirst()
            .orElse(null);
        if (identity == null) {
            throw new IllegalArgumentException("error.invalid_user");
        }

        // fetch provider
        WebAuthnCredentialsService service = webAuthnAuthority.getProvider(providerId);

        logger.debug("build registration attestationOptions for user {}", StringUtils.trimAllWhitespace(username));

        // build a new request via service
        WebAuthnRegistrationRequest request = service.startRegistration(username, reg);

        // store request
        String key = requestStore.store(request);
        if (logger.isTraceEnabled()) {
            logger.trace("request {}: {}", key, String.valueOf(request));
        }

        // build response
        WebAuthnRegistrationResponse response = new WebAuthnRegistrationResponse(
            key,
            request.getCredentialCreationInfo().getOptions()
        );

        return response;
    }

    /**
     * Validates the attestation generated using the Credential Creation Options
     * obtained through the {@link #generateAttestationOptions} controller
     */
    @Hidden
    @PostMapping(value = "/webauthn/attestations/{providerId}/{key}")
    public String verifyAttestation(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String key,
        @Valid AttestationResponse body
    ) throws NoSuchProviderException, WebAuthnAuthenticationException, RegistrationException, NoSuchUserException {
        // first check uuid vs user
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }

        // fetch registration
        WebAuthnRegistrationRequest request = requestStore.consume(key);
        if (request == null) {
            // no registration in progress with this key
            throw new IllegalArgumentException();
        }

        // fetch internal identities
        Set<UserIdentity> identities = user
            .getIdentities()
            .stream()
            .filter(i -> (i instanceof InternalUserIdentity))
            .collect(Collectors.toSet());

        // pick matching by username and ignore provider
        String username = request.getStartRequest().getUsername();
        UserIdentity identity = identities
            .stream()
            .filter(i -> i.getAccount().getUsername().equals(username))
            .findFirst()
            .orElse(null);
        if (identity == null) {
            throw new IllegalArgumentException("error.invalid_user");
        }

        // fetch provider
        WebAuthnCredentialsService service = webAuthnAuthority.getProvider(providerId);

        // register response
        // TODO implement audit
        request.setAttestationResponse(body);

        logger.debug("finish registration {} for user {}", key, StringUtils.trimAllWhitespace(username));

        // parse body
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc = null;
        try {
            pkc = PublicKeyCredential.parseRegistrationResponseJson(body.getAttestation());
        } catch (IOException e) {}

        if (logger.isTraceEnabled()) {
            logger.trace("pkc for {}: {}", key, String.valueOf(pkc));
        }

        if (pkc == null) {
            logger.error("invalid attestation for registration");
            throw new RegistrationException("invalid attestation");
        }

        // finish registration via service
        request = service.finishRegistration(username, request, pkc);
        String userHandle = request.getUserHandle();

        if (logger.isTraceEnabled()) {
            logger.trace("request {}: {}", key, String.valueOf(request));
        }

        // save successful registration as credential
        WebAuthnUserCredential credential = service.saveRegistration(
            username,
            request.getStartRequest().getDisplayName(),
            request
        );

        // register as new
        logger.debug(
            "registered credential {} for user {} via userHandle {}",
            credential.getCredentialId(),
            StringUtils.trimAllWhitespace(username),
            userHandle
        );

        if (logger.isTraceEnabled()) {
            logger.trace("credential {}: {}", credential.getCredentialId(), String.valueOf(credential));
        }

        String uuid = userHandle;
        return "redirect:/webauthn/credentials/" + providerId + "/" + uuid;
    }
}
