package it.smartcommunitylab.aac.controller;

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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.ClientManager;
import it.smartcommunitylab.aac.core.model.ClientCredentials;
import it.smartcommunitylab.aac.model.ClientApp;

/*
 * Base controller for client app
 */
@PreAuthorize("hasAuthority(this.authority)")
public class BaseClientAppController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ClientManager clientManager;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    public String getAuthority() {
        return Config.R_USER;
    }

    @GetMapping("/app/{realm}")
    public Collection<ClientApp> listApp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        logger.debug("list client apps for realm {}",
                StringUtils.trimAllWhitespace(realm));

        return clientManager.listClientApps(realm);
    }

    @GetMapping("/app/{realm}/{clientId}")
    public ClientApp getApp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("get client app {} for realm {}",
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(realm));

        return clientManager.getClientApp(realm, clientId);
    }

    /*
     * Management
     */
    @PostMapping("/app/{realm}")
    public ClientApp registerApp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @NotNull @Valid ClientApp app) throws NoSuchRealmException {
        logger.debug("register client app for realm {}",
                StringUtils.trimAllWhitespace(realm));

        app.setRealm(realm);
        if (logger.isTraceEnabled()) {
            logger.trace("app bean: " + StringUtils.trimAllWhitespace(app.toString()));
        }

        return clientManager.registerClientApp(realm, app);
    }

    @PutMapping("/app/{realm}/{clientId}")
    public ClientApp updateApp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @RequestBody @Valid @NotNull ClientApp app) throws NoSuchClientException, NoSuchRealmException {
        logger.debug("update client app {} for realm {}",
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(realm));

        app.setRealm(realm);
        if (logger.isTraceEnabled()) {
            logger.trace("app bean: " + StringUtils.trimAllWhitespace(app.toString()));
        }

        return clientManager.updateClientApp(realm, clientId, app);
    }

    @DeleteMapping("/app/{realm}/{clientId}")
    public void deleteApp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("delete client app {}  for realm {}",
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(realm));

        clientManager.deleteClientApp(realm, clientId);
    }

    @PutMapping("/app/{realm}")
    public ClientApp importApp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam("file") @Valid @NotNull @NotBlank MultipartFile file) throws Exception {
        logger.debug("import client app for realm {}",
                StringUtils.trimAllWhitespace(realm));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("empty file");
        }

        if (file.getContentType() == null) {
            throw new IllegalArgumentException("invalid file");
        }

        if (!SystemKeys.MEDIA_TYPE_YAML.toString().equals(file.getContentType())
                && !SystemKeys.MEDIA_TYPE_YML.toString().equals(file.getContentType())) {
            throw new IllegalArgumentException("invalid file");
        }

        try {
            ClientApp reg = yamlObjectMapper.readValue(file.getInputStream(), ClientApp.class);
            reg.setRealm(realm);
            if (logger.isTraceEnabled()) {
                logger.trace("app bean: " + StringUtils.trimAllWhitespace(reg.toString()));
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
    public ClientCredentials getAppCredentials(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("get credentials for client app {} for realm {}",
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(realm));

        return clientManager.getClientCredentials(realm, clientId);
    }

    @PutMapping("/app/{realm}/{clientId}/credentials")
    public ClientCredentials getAppCredentials(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @RequestBody @NotNull Map<String, Object> credentials)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("set credentials for client app {} for realm {}",
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(realm));

        return clientManager.setClientCredentials(realm, clientId, credentials);
    }

    @DeleteMapping("/app/{realm}/{clientId}/credentials")
    public ClientCredentials resetAppCredentials(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("reset credentials for client app {} for realm {}",
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(realm));

        return clientManager.resetClientCredentials(realm, clientId);
    }

    /*
     * Configuration schema
     */
    @GetMapping("/app/{realm}/{clientId}/schema")
    public JsonSchema getAppConfigurationSchema(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("get configuration schema for client app {} for realm {}",
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(realm));

        return clientManager.getClientConfigurationSchema(realm, clientId);
    }

}
