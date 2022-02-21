package it.smartcommunitylab.aac.webauthn.controller;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.validation.Valid;

import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
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
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnAttestationResponse;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationResponse;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationStartRequest;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnRpService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnRpServiceRegistrationRepository;

/**
 * Manages the endpoints connected to the registration ceremony of WebAuthn.
 * 
 * The registration ceremony is used to register new WebAuthn credentials on
 * this server.
 */
@Controller
@RequestMapping
public class WebAuthnRegistrationController {

    @Autowired
    private WebAuthnRpServiceRegistrationRepository webAuthnRpServiceRegistrationRepository;

    @Autowired
    private ProviderRepository<WebAuthnIdentityProviderConfig> registrationRepository;

    private static Pattern pattern = Pattern.compile(SystemKeys.SLUG_PATTERN);

    /**
     * Serves the page to register a new WebAuthn credential.
     * Ensure the request mapping value matches the one returned from
     * {@link WebAuthnIdentityService#getRegistrationUrl}
     */
    @Hidden
    @RequestMapping(value = "/auth/webauthn/register/{providerId}", method = RequestMethod.GET)
    public String registrationPage(@PathVariable("providerId") String providerId) {
        WebAuthnIdentityProviderConfig provider = registrationRepository.findByProviderId(providerId);
        if (provider == null) {
            throw new RegistrationException("No provider with id " + providerId);
        }
        if (!provider.getConfigMap().isEnableRegistration()) {
            throw new IllegalArgumentException();
        }
        return "webauthn/register";
    }

    /**
     * Starts a new WebAuthn registration ceremony by generating a new Credential
     * Creation Options object and returning it to the user.
     * 
     * The challenge and the information about the ceremony are temporarily stored
     * in the session.
     */
    @Hidden
    @PostMapping(value = "/auth/webauthn/attestationOptions/{providerId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public WebAuthnRegistrationResponse generateAttestationOptions(
            @RequestBody @Valid WebAuthnRegistrationStartRequest body,
            @PathVariable("providerId") String providerId) {
        try {
            WebAuthnIdentityProviderConfig provider = registrationRepository.findByProviderId(providerId);
            if (provider == null) {
                throw new RegistrationException("No provider with id " + providerId);
            }
            if (!provider.getConfigMap().isEnableRegistration()) {
                throw new IllegalArgumentException();
            }
            WebAuthnRpService rps = webAuthnRpServiceRegistrationRepository.get(providerId);
            String username = body.getUsername();
            if (!isValidUsername(username)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username");
            }

            String displayName = body.getDisplayName();
            if (!isValidDisplayName(displayName)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid displayName");
            }

            final WebAuthnRegistrationResponse response = rps.startRegistration(
                    username,
                    displayName);
            return response;
        } catch (ExecutionException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validates the attestation generated using the Credential Creation
     * Options obtained through the {@link #generateAttestationOptions} controller
     */
    @Hidden
    @PostMapping(value = "/auth/webauthn/attestations/{providerId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String verifyAttestation(@RequestBody @Valid WebAuthnAttestationResponse body,
            @PathVariable("providerId") String providerId) { 
        try {
            final boolean canRegister = registrationRepository.findByProviderId(providerId)
                    .getConfigMap()
                    .isEnableRegistration();
            if (!canRegister) {
                throw new RegistrationException("registration is disabled");
            }
            WebAuthnRpService rps = webAuthnRpServiceRegistrationRepository.get(providerId);
            WebAuthnIdentityProviderConfig providerCfg = registrationRepository.findByProviderId(providerId);
            String realm = providerCfg.getRealm();

            String key = body.getKey();

            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc = PublicKeyCredential
                    .parseRegistrationResponseJson(body.toJson());

            final String authenticatedUser = rps.finishRegistration(pkc, realm, key);
            if (!StringUtils.hasText(authenticatedUser)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid attestation");
            }
            return "Welcome " + authenticatedUser + ". Next step is to authenticate your session";
        } catch (IOException | ExecutionException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Invalid attestation");
        }
    }

    /**
     * Checks if the provided object can be used as a valid username
     */
    private boolean isValidUsername(String username) {
        return pattern.matcher(username).matches();
    }

    /**
     * Checks if the provided object can be used as a valid display name
     */
    private boolean isValidDisplayName(String candidate) {
        return pattern.matcher(candidate).matches();
    }
}
