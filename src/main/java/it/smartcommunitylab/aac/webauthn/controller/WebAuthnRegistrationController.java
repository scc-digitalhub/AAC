package it.smartcommunitylab.aac.webauthn.controller;

import java.util.LinkedHashMap;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;

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
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityService;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnSubjectResolver;

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
    private WebAuthnIdentityAuthority webAuthnAuthority;

    /**
     * Serves the page to register a new WebAuthn credential.
     * Ensure the request mapping value matches the one returned from
     * {@link WebAuthnIdentityService#getRegistrationUrl}
     */
    @Hidden
    @RequestMapping(value = "/auth/webauthn/register/{providerId}", method = RequestMethod.GET)
    public String registrationPage(@PathVariable("providerId") String providerId) {
        WebAuthnIdentityService idp = webAuthnAuthority.getIdentityService(providerId);
        if (!idp.canRegister()) {
            throw new RegistrationException("registration is disabled");
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
    public String generateAttestationOptions(@RequestBody LinkedHashMap<String, Object> body,
            @PathVariable("providerId") String providerId) {
        try {
            // resolve provider
            WebAuthnIdentityService idp = webAuthnAuthority.getIdentityService(providerId);

            if (!idp.canRegister()) {
                throw new RegistrationException("registration is disabled");
            }

            String username;
            final HttpSession session = ControllerUtils.getSession();
            Object _userName = body.get("username");
            if (ControllerUtils.isValidUsername(_userName)) {
                username = (String) _userName;
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username");
            }
            Optional<String> displayName = Optional.empty();

            Object _reqDisplayName = body.getOrDefault("displayName", null);
            if (ControllerUtils.isValidDisplayName(_reqDisplayName)) {
                displayName = Optional.of((String) _reqDisplayName);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid displayName");
            }

            final WebAuthnSubjectResolver subjectResolver = idp.getSubjectResolver();
            Subject resolvedUserId = subjectResolver
                    .resolveByUserId(username);
            final Optional<Subject> subjectOpt = Optional
                    .ofNullable(resolvedUserId);
            PublicKeyCredentialCreationOptions options = idp.startRegistration(username,
                    session.getId(),
                    displayName,
                    subjectOpt);
            return options.toCredentialsCreateJson();
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
    public String verifyAttestation(@RequestBody LinkedHashMap<String, Object> body,
            @PathVariable("providerId") String providerId) {
        final ResponseStatusException invalidAttestationException = new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid attestation");
        try {
            // resolve provider
            WebAuthnIdentityService idp = webAuthnAuthority.getIdentityService(providerId);

            if (!idp.canRegister()) {
                throw new RegistrationException("registration is disabled");
            }

            Object attestationMap = body.get("attestation");
            ObjectMapper mapper = new ObjectMapper();
            if (attestationMap == null || !(attestationMap instanceof LinkedHashMap)) {
                throw invalidAttestationException;
            }
            String attestationString = mapper.writeValueAsString(attestationMap);
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc = PublicKeyCredential
                    .parseRegistrationResponseJson(attestationString);
            final HttpSession session = ControllerUtils.getSession();
            final Optional<String> authenticatedUser = idp.finishRegistration(pkc, session.getId());
            if (authenticatedUser.isPresent()) {
                // TODO: civts, time to authenticate the session
                return "Welcome " + authenticatedUser.get() + ". Next step is to authenticate your session";
            }
        } catch (Exception e) {
        }
        throw invalidAttestationException;
    }
}
