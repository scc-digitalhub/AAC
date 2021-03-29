package it.smartcommunitylab.aac.controller;

import java.util.Collection;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.ClientManager;
import it.smartcommunitylab.aac.core.model.ClientCredentials;
import it.smartcommunitylab.aac.model.ClientApp;

/*
 * API controller for clientApp
 * 
 * split from UI controller to ensure proper authentication is used,
 * also we want to expose client management
 */
@RestController
@RequestMapping("api/app")
//@Validated
public class ClientAppController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    //
    // TODO permissions
    // TODO rest exception handler (via controllerAdvice + custom ApiError model)
    // TODO bean validation
    //

    @Autowired
    private ClientManager clientManager;

    @GetMapping("{realm}")
    public Collection<ClientApp> listApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) {

        return clientManager.listClientApps(realm);

    }

    @GetMapping("{realm}/{clientId}")
    public ClientApp getApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException {

        return clientManager.getClientApp(realm, clientId);

    }

    /*
     * Management
     */
    @PostMapping("{realm}")
    public ClientApp registerApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid ClientApp app) {
        app.setRealm(realm);
        return clientManager.registerClientApp(realm, app);
    }

    @PutMapping("{realm}/{clientId}")
    public ClientApp updateApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @RequestBody @Valid ClientApp app) throws NoSuchClientException {
        app.setRealm(realm);
        return clientManager.updateClientApp(realm, clientId, app);
    }

    @DeleteMapping("{realm}/{clientId}")
    public void deleteApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException {

        clientManager.deleteClient(realm, clientId);

    }

    /*
     * Credentials
     */

    @GetMapping("{realm}/{clientId}/credentials")
    public ClientCredentials getAppCredentials(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException {

        return clientManager.getClientCredentials(realm, clientId);

    }

    @PutMapping("{realm}/{clientId}/credentials")
    public ClientCredentials getAppCredentials(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @RequestBody Map<String, Object> credentials)
            throws NoSuchClientException {

        return clientManager.setClientCredentials(realm, clientId, credentials);

    }

    @DeleteMapping("{realm}/{clientId}/credentials")
    public ClientCredentials resetAppCredentials(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException {

        return clientManager.resetClientCredentials(realm, clientId);

    }

    /*
     * Configuration models
     */
    @GetMapping("schema/{type}")
    public JsonSchema getConfigurationSchema(
            @PathVariable(required = true) String type) throws IllegalArgumentException {

        return clientManager.getConfigurationSchema(type);
    }

}
