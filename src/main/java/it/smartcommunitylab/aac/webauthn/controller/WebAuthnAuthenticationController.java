package it.smartcommunitylab.aac.webauthn.controller;

import java.util.LinkedHashMap;
import java.util.Optional;

import javax.validation.Valid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.icu.impl.Pair;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
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
import it.smartcommunitylab.aac.webauthn.model.WebAuthnAssertionResponse;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnLoginResponse;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityService;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnRpServiceReigistrationRepository;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnRpService;

/**
 * Manages the endpoints connected to the authentication ceremony of WebAuthn.
 * 
 * The authentication ceremony is used to authenticate the client by verifying
 * that it owns a credential that was previously registered on this server
 * (thanks to the {@link WebAuthnRegistrationController}).
 */
@Controller
@RequestMapping
public class WebAuthnAuthenticationController {

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private WebAuthnRpServiceReigistrationRepository webAuthnRpServiceReigistrationRepository;

    /**
     * Serves the page to start a new WebAuthn authentication ceremony.
     * Ensure the request mapping value matches the one returned from
     * {@link WebAuthnIdentityService#getLoginUrl}
     */
    @Hidden
    @RequestMapping(value = "/auth/webauthn/login/{providerId}", method = RequestMethod.GET)
    public String authenticatePage() {
        return "webauthn/authenticate";
    }

    /**
     * Starts a new WebAuthn authentication ceremony by generating a new Credential
     * Request Options object and returning it to the user.
     * 
     * The challenge and the information about the ceremony are temporarily stored
     * in the session.
     */
    @Hidden
    @PostMapping(value = "/auth/webauthn/assertionOptions/{providerId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String generateAssertionOptions(@RequestBody LinkedHashMap<String, Object> body,
            @PathVariable("providerId") String providerId) {
        try {
            final WebAuthnRpService rps = webAuthnRpServiceReigistrationRepository.getOrCreate(providerId);
            final String realm = webAuthnRpServiceReigistrationRepository.getRealm(providerId);

            String username;
            Object _userName = body.get("username");
            if (UsernameValidator.isValidUsername(_userName)) {
                username = (String) _userName;
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username");
            }

            Pair<AssertionRequest, String> reqAndKey = rps.startLogin(username, realm);
            final WebAuthnLoginResponse response = new WebAuthnLoginResponse();
            response.setAssertionRequest(reqAndKey.first);
            response.setKey(reqAndKey.second);
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validates the assertion generated using the Credential Request
     * Options obtained through the {@link #generateAssertionOptions} controller
     */
    @Hidden
    @RequestMapping(value = "/auth/webauthn/assertions/{providerId}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String verifyAssertion(@RequestBody @Valid WebAuthnAssertionResponse body,
            @PathVariable("providerId") String providerId) {
        Object assertionJSON = body.getAssertion();
        final String key = body.getKey();
        if (assertionJSON == null || !(assertionJSON instanceof LinkedHashMap)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid assertion");
        }
        try {
            final WebAuthnRpService rps = webAuthnRpServiceReigistrationRepository.getOrCreate(providerId);

            String assertionString = mapper.writeValueAsString(assertionJSON);
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc = PublicKeyCredential
                    .parseAssertionResponseJson((String) assertionString);

            final Optional<String> authenticatedUser = rps.finishLogin(pkc, key);
            if (authenticatedUser.isPresent()) {
                return "Welcome " + authenticatedUser.get();
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid assertion");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid assertion");
        }
    }
}