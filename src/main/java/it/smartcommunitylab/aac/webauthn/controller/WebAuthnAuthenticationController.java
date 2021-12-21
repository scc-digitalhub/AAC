package it.smartcommunitylab.aac.webauthn.controller;

import java.util.LinkedHashMap;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityService;

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

    static final String currentAuthenticationKeyFieldName = "currentWebAuthnAuthentication";

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
    @PostMapping(value = "/auth/webauthn/assertionOptions/{providerId}", consumes = {
            MediaType.APPLICATION_JSON_VALUE })
    public String generateAssertionOptions(@RequestBody LinkedHashMap<String, Object> body) {

        // TODO: civts, generate credential request options
        return "TODO generateAssertionOptions";
    }

    /**
     * Validates the assertion generated using the Credential Request
     * Options obtained through the {@link #generateAssertionOptions} controller
     */
    @Hidden
    @RequestMapping(value = "/auth/webauthn/assertions/{providerId}", method = RequestMethod.POST)
    public String verifyAssertion() {
        // TODO: civts, verify assertion
        return "TODO verifyAssertion";
    }
}
