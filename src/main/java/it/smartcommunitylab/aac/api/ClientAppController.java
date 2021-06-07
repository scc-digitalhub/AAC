package it.smartcommunitylab.aac.api;

import java.util.Collection;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.ApiClientAppScope;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
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
@RequestMapping("api")
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

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    @GetMapping("/app/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')) and hasAuthority('SCOPE_"
            + ApiClientAppScope.SCOPE + "')")
    public Collection<ClientApp> listApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {
        logger.debug("list client apps for realm " + String.valueOf(realm));

        return clientManager.listClientApps(realm);

    }

    @GetMapping("/app/{realm}/{clientId}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')) and hasAuthority('SCOPE_"
            + ApiClientAppScope.SCOPE + "')")
    public ClientApp getApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("get client app " + String.valueOf(clientId) + " for realm " + String.valueOf(realm));

        return clientManager.getClientApp(realm, clientId);

    }

    /*
     * Management
     */
    @PostMapping("/app/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')) and hasAuthority('SCOPE_"
            + ApiClientAppScope.SCOPE + "')")
    public ClientApp registerApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid ClientApp app) throws NoSuchRealmException {

        app.setRealm(realm);

        logger.debug("register client app for realm " + String.valueOf(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("app bean: " + String.valueOf(app));
        }

        return clientManager.registerClientApp(realm, app);
    }

    @PutMapping("/app/{realm}/{clientId}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')) and hasAuthority('SCOPE_"
            + ApiClientAppScope.SCOPE + "')")
    public ClientApp updateApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @RequestBody @Valid ClientApp app) throws NoSuchClientException, NoSuchRealmException {

        app.setRealm(realm);

        logger.debug("update client app " + String.valueOf(clientId) + " for realm " + String.valueOf(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("app bean: " + String.valueOf(app));
        }

        return clientManager.updateClientApp(realm, clientId, app);
    }

    @DeleteMapping("/app/{realm}/{clientId}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')) and hasAuthority('SCOPE_"
            + ApiClientAppScope.SCOPE + "')")
    public void deleteApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("delete client app " + String.valueOf(clientId) + " for realm " + String.valueOf(realm));

        clientManager.deleteClientApp(realm, clientId);

    }

    @PutMapping("/app/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')) and hasAuthority('SCOPE_"
            + ApiClientAppScope.SCOPE + "')")
    public ClientApp importApp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam("file") @Valid @NotNull @NotBlank MultipartFile file) throws Exception {
        logger.debug("import client app for realm " + String.valueOf(realm));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("empty file");
        }

        if (file.getContentType() != null &&
                (!file.getContentType().equals(SystemKeys.MEDIA_TYPE_YAML.toString()) &&
                        !file.getContentType().equals(SystemKeys.MEDIA_TYPE_YML.toString()))) {
            throw new IllegalArgumentException("invalid file");
        }
        try {
            ClientApp reg = yamlObjectMapper.readValue(file.getInputStream(), ClientApp.class);
            reg.setRealm(realm);
            if (logger.isTraceEnabled()) {
                logger.trace("app bean: " + String.valueOf(reg));
            }
            return clientManager.registerClientApp(realm, reg);
        } catch (Exception e) {
            logger.error("import client app error: " + e.getMessage());

            throw e;
        }

    }

    /*
     * Credentials
     */

    @GetMapping("/app/{realm}/{clientId}/credentials")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')) and hasAuthority('SCOPE_"
            + ApiClientAppScope.SCOPE + "')")
    public ClientCredentials getAppCredentials(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug(
                "get credentials for client app " + String.valueOf(clientId) + " for realm " + String.valueOf(realm));

        return clientManager.getClientCredentials(realm, clientId);

    }

    @PutMapping("/app/{realm}/{clientId}/credentials")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')) and hasAuthority('SCOPE_"
            + ApiClientAppScope.SCOPE + "')")
    public ClientCredentials getAppCredentials(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @RequestBody Map<String, Object> credentials)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug(
                "set credentials for client app " + String.valueOf(clientId) + " for realm " + String.valueOf(realm));
        return clientManager.setClientCredentials(realm, clientId, credentials);

    }

    @DeleteMapping("/app/{realm}/{clientId}/credentials")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')) and hasAuthority('SCOPE_"
            + ApiClientAppScope.SCOPE + "')")
    public ClientCredentials resetAppCredentials(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug(
                "reset credentials for client app " + String.valueOf(clientId) + " for realm " + String.valueOf(realm));
        return clientManager.resetClientCredentials(realm, clientId);

    }

    /*
     * Configuration schema
     */
    @GetMapping("/app_schema/{type}")
    public JsonSchema getConfigurationSchema(
            @PathVariable(required = true) @Valid @NotBlank String type) throws IllegalArgumentException {
        logger.debug(
                "get client app config schema for type" + String.valueOf(type));
        return clientManager.getConfigurationSchema(type);
    }

}
