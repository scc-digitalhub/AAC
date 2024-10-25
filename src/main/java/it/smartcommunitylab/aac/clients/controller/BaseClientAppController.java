/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.clients.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import io.swagger.v3.oas.annotations.Operation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.clients.ClientManager;
import it.smartcommunitylab.aac.clients.model.ClientCredentials;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.model.ClientApp;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping("/apps/{realm}")
    @Operation(summary = "list client apps from a given realm")
    public Page<ClientApp> listClientApp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String[] providers,
        Pageable pageRequest
    ) throws NoSuchRealmException {
        logger.debug("list client apps for realm {}", StringUtils.trimAllWhitespace(realm));
        
        if(providers == null || providers.length == 0) {
            return clientManager.searchClientApps(realm, q, pageRequest);
        } else {
            //manually filter, breaks pagination
            Set<String> ps = new HashSet<>(Arrays.asList(providers));
            Page<ClientApp> page = clientManager.searchClientApps(realm, q, pageRequest);
            return PageableExecutionUtils.getPage(
                page.getContent().stream().filter(a -> Arrays.asList(a.getProviders()).stream().anyMatch(p -> ps.contains(p))).toList(),
                pageRequest,
                () -> page.getTotalElements()
            );
           
        }
    }

    @GetMapping("/apps/{realm}/{clientId}")
    @Operation(summary = "get a specific client app from a given realm")
    public ClientApp getClientApp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId
    ) throws NoSuchClientException, NoSuchRealmException {
        logger.debug(
            "get client app {} for realm {}",
            StringUtils.trimAllWhitespace(clientId),
            StringUtils.trimAllWhitespace(realm)
        );

        return clientManager.getClientApp(realm, clientId);
    }

    /*
     * Management
     */
    @PostMapping("/apps/{realm}")
    @Operation(summary = "register a new client app in a given realm")
    public ClientApp registerClientApp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestBody @NotNull @Valid ClientApp app
    ) throws NoSuchRealmException {
        logger.debug("register client app for realm {}", StringUtils.trimAllWhitespace(realm));

        // enforce realm match
        app.setRealm(realm);

        if (logger.isTraceEnabled()) {
            logger.trace("app bean: {}", StringUtils.trimAllWhitespace(app.toString()));
        }

        return clientManager.registerClientApp(realm, app);
    }

    @PutMapping("/apps/{realm}/{clientId}")
    @Operation(summary = "update a specific client app in a given realm")
    public ClientApp updateClientApp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
        @RequestBody @Valid @NotNull ClientApp app
    ) throws NoSuchClientException, NoSuchRealmException {
        logger.debug(
            "update client app {} for realm {}",
            StringUtils.trimAllWhitespace(clientId),
            StringUtils.trimAllWhitespace(realm)
        );

        // enforce realm match
        app.setRealm(realm);

        if (logger.isTraceEnabled()) {
            logger.trace("app bean: {}", StringUtils.trimAllWhitespace(app.toString()));
        }

        return clientManager.updateClientApp(realm, clientId, app);
    }

    @DeleteMapping("/apps/{realm}/{clientId}")
    @Operation(summary = "delete a specific client app from a given realm")
    public void deleteClientApp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId
    ) throws NoSuchClientException, NoSuchRealmException {
        logger.debug(
            "delete client app {}  for realm {}",
            StringUtils.trimAllWhitespace(clientId),
            StringUtils.trimAllWhitespace(realm)
        );

        clientManager.deleteClientApp(realm, clientId);
    }

    /*
     * Credentials
     */

    @GetMapping("/apps/{realm}/{clientId}/credentials")
    @Operation(summary = "list credentials for a client app in a given realm")
    public Collection<ClientCredentials> listClientAppCredentials(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId
    ) throws NoSuchClientException, NoSuchRealmException {
        logger.debug(
            "get credentials for client app {} for realm {}",
            StringUtils.trimAllWhitespace(clientId),
            StringUtils.trimAllWhitespace(realm)
        );

        return clientManager.getClientCredentials(realm, clientId);
    }

    @GetMapping("/apps/{realm}/{clientId}/credentials/{credentialsId}")
    @Operation(summary = "get a specific credential for a client app in a given realm")
    public ClientCredentials getClientAppCredentials(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String credentialsId
    ) throws NoSuchClientException, NoSuchRealmException {
        logger.debug(
            "get credentials for client app {} for realm {}",
            StringUtils.trimAllWhitespace(clientId),
            StringUtils.trimAllWhitespace(realm)
        );

        return clientManager.getClientCredentials(realm, clientId, credentialsId);
    }

    @PutMapping("/apps/{realm}/{clientId}/credentials/{credentialsId}")
    @Operation(summary = "update credentials for a client app in a given realm")
    public ClientCredentials setClientAppCredentials(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String credentialsId,
        @RequestBody @NotNull Map<String, Serializable> credentials
    ) throws NoSuchClientException, NoSuchRealmException {
        logger.debug(
            "set/reset credentials for client app {} for realm {}",
            StringUtils.trimAllWhitespace(clientId),
            StringUtils.trimAllWhitespace(realm)
        );

        // TODO support set by parsing map
        return clientManager.resetClientCredentials(realm, clientId, credentialsId);
    }

    @DeleteMapping("/apps/{realm}/{clientId}/credentials/{credentialsId}")
    @Operation(summary = "delete a specific credential for a client app in a given realm")
    public void removeClientAppCredentials(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String credentialsId
    ) throws NoSuchClientException, NoSuchRealmException {
        logger.debug(
            "reset credentials for client app {} for realm {}",
            StringUtils.trimAllWhitespace(clientId),
            StringUtils.trimAllWhitespace(realm)
        );

        clientManager.removeClientCredentials(realm, clientId, credentialsId);
    }

    /*
     * Configuration schema
     */
    @GetMapping("/apps/{realm}/{clientId}/schema")
    @Operation(summary = "get client configuration schema")
    public JsonSchema getClientAppConfigurationSchema(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId
    ) throws NoSuchClientException, NoSuchRealmException {
        logger.debug(
            "get configuration schema for client app {} for realm {}",
            StringUtils.trimAllWhitespace(clientId),
            StringUtils.trimAllWhitespace(realm)
        );

        return clientManager.getClientConfigurationSchema(realm, clientId);
    }
}
