package it.smartcommunitylab.aac.webauthn.controller;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.yubico.webauthn.AssertionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnAuthenticationStartRequest;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnLoginResponse;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnRpService;
import it.smartcommunitylab.aac.webauthn.store.InMemoryWebAuthnRequestStore;

/**
 * Manages the endpoint connected to the authentication ceremony of WebAuthn.
 * 
 * The authentication ceremony is used to authenticate the client by verifying
 * that it owns a credential that was previously registered on this server
 * (thanks to the {@link WebAuthnRegistrationController}).
 */
@Controller
@RequestMapping
public class WebAuthnAuthenticationController {

    @Autowired
    private WebAuthnRpService rpService;

    @Autowired
    private InMemoryWebAuthnRequestStore requestStore;

    /**
     * Serves the page to start a new WebAuthn authentication ceremony. Ensure the
     * request mapping value matches the one returned from
     * {@link WebAuthnIdentityService#getLoginUrl}
     */
    @Hidden
    @RequestMapping(value = "/auth/webauthn/_form/{providerId}", method = RequestMethod.GET)
    public String authenticatePage(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId) {
        return "webauthn/authenticate";
    }

    /**
     * Starts a new WebAuthn authentication ceremony by generating a new Credential
     * Request Options object and returning it to the user.
     * 
     * The challenge and the information about the ceremony are temporarily stored
     * in the session.
     * 
     * @throws NoSuchProviderException
     * @throws NoSuchUserException
     */
    @Hidden
    @PostMapping(value = "/auth/webauthn/_assertionOptions/{providerId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public WebAuthnLoginResponse generateAssertionOptions(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @RequestBody @Valid WebAuthnAuthenticationStartRequest body)
            throws NoSuchProviderException, NoSuchUserException {

        // build request for user
        String username = body.getUsername();
        // TODO evaluate displayName support

        AssertionRequest assertionRequest = rpService.startLogin(providerId,  username);

        // store request
        String key = requestStore.store(assertionRequest);

        // build response
        WebAuthnLoginResponse response = new WebAuthnLoginResponse();
        response.setAssertionRequest(assertionRequest);
        response.setKey(key);
        return response;
    }

}
