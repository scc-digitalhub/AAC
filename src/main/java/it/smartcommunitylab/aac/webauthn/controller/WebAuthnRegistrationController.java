package it.smartcommunitylab.aac.webauthn.controller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.Valid;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnAttestationResponse;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationResponse;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationStartRequest;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityService;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnRpServiceReigistrationRepository;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnSubjectResolver;
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

    private final ProviderRepository<WebAuthnIdentityProviderConfig> registrationRepository;

    public WebAuthnRegistrationController(
            ProviderRepository<WebAuthnIdentityProviderConfig> registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private WebAuthnRpServiceReigistrationRepository webAuthnRpServiceReigistrationRepository;

    @Autowired
    private WebAuthnIdentityAuthority webAuthnIdentityAuthority;

    /**
     * Serves the page to register a new WebAuthn credential.
     * Ensure the request mapping value matches the one returned from
     * {@link WebAuthnIdentityService#getRegistrationUrl}
     */
    @Hidden
    @RequestMapping(value = "/auth/webauthn/register/{providerId}", method = RequestMethod.GET)
    public String registrationPage(@PathVariable("providerId") String providerId) {
        try {
            final boolean canRegister = registrationRepository.findByProviderId(providerId)
                    .getConfigMap()
                    .isEnableRegistration();
            if (!canRegister) {
                throw new RegistrationException("registration is disabled");
            }
            return "webauthn/register";
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
            final boolean canRegister = registrationRepository.findByProviderId(providerId)
                    .getConfigMap()
                    .isEnableRegistration();
            if (!canRegister) {
                throw new RegistrationException("registration is disabled");
            }
            WebAuthnRpService rps = webAuthnRpServiceReigistrationRepository.get(providerId);
            String username = body.getUsername();
            if (!isValidUsername(username)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username");
            }

            String displayName = body.getDisplayName();
            if (!isValidDisplayName(displayName)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid displayName");
            }

            final WebAuthnIdentityService webAuthnIdentityService = webAuthnIdentityAuthority
                    .getIdentityService(providerId);
            final WebAuthnSubjectResolver subjectResolver = webAuthnIdentityService.getSubjectResolver();
            Subject subject = subjectResolver
                    .resolveByUserId(username);
            WebAuthnIdentityProviderConfig providerCfg = registrationRepository.findByProviderId(providerId);
            String realm = providerCfg.getRealm();
            final WebAuthnRegistrationResponse response = rps.startRegistration(
                    username,
                    realm,
                    displayName,
                    subject);
            return response;
        } catch (Exception e) {
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
        final ResponseStatusException invalidAttestationException = new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid attestation");
        try {
            final boolean canRegister = registrationRepository.findByProviderId(providerId)
                    .getConfigMap()
                    .isEnableRegistration();
            if (!canRegister) {
                throw new RegistrationException("registration is disabled");
            }
            WebAuthnRpService rps = webAuthnRpServiceReigistrationRepository.get(providerId);
            WebAuthnIdentityProviderConfig providerCfg = registrationRepository.findByProviderId(providerId);
            String realm = providerCfg.getRealm();

            String key = body.getKey();
            String attestationString = mapper.writeValueAsString(body.getAttestation());
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc = PublicKeyCredential
                    .parseRegistrationResponseJson(attestationString);

            final String authenticatedUser = rps.finishRegistration(pkc, realm, key);
            if (!StringUtils.hasText(authenticatedUser)) {
                throw invalidAttestationException;
            }
            return "Welcome " + authenticatedUser + ". Next step is to authenticate your session";
        } catch (Exception e) {
            throw invalidAttestationException;
        }
    }

    /**
     * Checks if the provided object can be used as a valid username
     */
    private boolean isValidUsername(Object username) {
        if (!(username instanceof String)) {
            return false;
        }
        Pattern pattern = Pattern.compile("^\\w{3,30}$");
        Matcher matcher = pattern.matcher((String) username);
        return matcher.find();
    }

    /**
     * Checks if the provided object can be used as a valid display name
     */
    private boolean isValidDisplayName(Object candidate) {
        if (!(candidate instanceof String)) {
            return false;
        }
        Pattern pattern = Pattern.compile("^[\\w ]{3,30}$");
        Matcher matcher = pattern.matcher((String) candidate);
        return matcher.find();
    }
}
