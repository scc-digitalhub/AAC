package it.smartcommunitylab.aac.webauthn.controller;

import java.util.LinkedHashMap;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
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

    @Autowired
    private WebAuthnIdentityAuthority webAuthnAuthority;

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
            // resolve provider
            WebAuthnIdentityService idp = webAuthnAuthority.getIdentityService(providerId);

            String username;
            final HttpSession session = ControllerUtils.getSession();
            Object _userName = body.get("username");
            if (ControllerUtils.isValidUsername(_userName)) {
                username = (String) _userName;
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username");
            }

            AssertionRequest req = idp.startLogin(username,
                    session.getId());
            return req.toCredentialsGetJson();
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
    public String verifyAssertion(@RequestBody LinkedHashMap<String, Object> body,
            @PathVariable("providerId") String providerId) {
        Object assertionJSON = body.get("assertion");
        if (assertionJSON == null || !(assertionJSON instanceof LinkedHashMap)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid assertion");
        }
        try {
            WebAuthnIdentityService idp = webAuthnAuthority.getIdentityService(providerId);

            ObjectMapper mapper = new ObjectMapper();
            String assertionString = mapper.writeValueAsString(assertionJSON);
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc = PublicKeyCredential
                    .parseAssertionResponseJson((String) assertionString);
            final HttpSession session = ControllerUtils.getSession();

            final Optional<String> authenticatedUser = idp.finishLogin(pkc, session.getId());
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
