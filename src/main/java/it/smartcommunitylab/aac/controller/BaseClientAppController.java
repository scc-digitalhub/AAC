package it.smartcommunitylab.aac.controller;

import java.io.Serializable;
import java.util.Collection;

import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
public class BaseClientAppController implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected ClientManager clientManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(clientManager, "client manager is required");
    }

    @Autowired
    public void setClientManager(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

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

    /*
     * Credentials
     */

    @GetMapping("/app/{realm}/{clientId}/credentials")
    public Collection<ClientCredentials> listAppCredentials(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("get credentials for client app {} for realm {}",
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(realm));

        return clientManager.getClientCredentials(realm, clientId);
    }

    @GetMapping("/app/{realm}/{clientId}/credentials/{credentialsId}")
    public ClientCredentials getAppCredentials(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String credentialsId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("get credentials for client app {} for realm {}",
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(realm));

        return clientManager.getClientCredentials(realm, clientId, credentialsId);
    }

    @PutMapping("/app/{realm}/{clientId}/credentials/{credentialsId}")
    public ClientCredentials setAppCredentials(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String credentialsId,
            @RequestBody @NotNull Map<String, Serializable> credentials)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("set/reset credentials for client app {} for realm {}",
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(realm));

        // TODO support set by parsing map
        return clientManager.resetClientCredentials(realm, clientId, credentialsId);
    }

    @DeleteMapping("/app/{realm}/{clientId}/credentials/{credentialsId}")
    public void removeAppCredentials(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String credentialsId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("reset credentials for client app {} for realm {}",
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(realm));

        clientManager.removeClientCredentials(realm, clientId, credentialsId);
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
