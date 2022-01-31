package it.smartcommunitylab.aac.webauthn.controller;

import java.util.LinkedHashMap;
import org.springframework.util.StringUtils;

import javax.validation.Valid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAuthenticationException;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnAttestationResponse;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationResponse;
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
            final boolean canRegister = webAuthnRpServiceReigistrationRepository.getProviderConfig(providerId)
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
    public String generateAttestationOptions(@RequestBody LinkedHashMap<String, Object> body,
            @PathVariable("providerId") String providerId) {
        try {
            final boolean canRegister = webAuthnRpServiceReigistrationRepository.getProviderConfig(providerId)
                    .getConfigMap()
                    .isEnableRegistration();
            if (!canRegister) {
                throw new RegistrationException("registration is disabled");
            }
            WebAuthnRpService rps = webAuthnRpServiceReigistrationRepository.getOrCreate(providerId);

            String username;
            Object _userName = body.get("username");
            if (UsernameValidator.isValidUsername(_userName)) {
                username = (String) _userName;
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username");
            }
            String displayName = null;

            Object _reqDisplayName = body.getOrDefault("displayName", null);
            if (UsernameValidator.isValidDisplayName(_reqDisplayName)) {
                displayName = (String) _reqDisplayName;
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid displayName");
            }

            final WebAuthnIdentityService webAuthnIdentityService = webAuthnIdentityAuthority
                    .getIdentityService(providerId);
            final WebAuthnSubjectResolver subjectResolver = webAuthnIdentityService.getSubjectResolver();
            Subject subject = subjectResolver
                    .resolveByUserId(username);
            final String realm = webAuthnRpServiceReigistrationRepository.getRealm(providerId);
            final WebAuthnRegistrationResponse response = rps.startRegistration(
                    username,
                    realm,
                    displayName,
                    subject);
            return mapper.writeValueAsString(response);
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
            final boolean canRegister = webAuthnRpServiceReigistrationRepository.getProviderConfig(providerId)
                    .getConfigMap()
                    .isEnableRegistration();
            if (!canRegister) {
                throw new RegistrationException("registration is disabled");
            }
            WebAuthnRpService rps = webAuthnRpServiceReigistrationRepository.getOrCreate(providerId);
            final String realm = webAuthnRpServiceReigistrationRepository.getRealm(providerId);

            String key = body.getKey();
            String attestationString = mapper.writeValueAsString(body.getAttestation());
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc = PublicKeyCredential
                    .parseRegistrationResponseJson(attestationString);

            try {
                final String authenticatedUser = rps.finishRegistration(pkc, realm, key);
                if(!StringUtils.hasText(authenticatedUser)){
                    throw invalidAttestationException;
                }
                return "Welcome " + authenticatedUser + ". Next step is to authenticate your session";
            } catch (WebAuthnAuthenticationException e) {
                throw invalidAttestationException;
            }

        } catch (Exception e) {
        }
        throw invalidAttestationException;
    }
}
