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

package it.smartcommunitylab.aac.oauth.service;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.nimbusds.jose.jwk.JWKSet;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.clients.service.ClientAppService;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.oauth.client.OAuth2Client;
import it.smartcommunitylab.aac.oauth.client.OAuth2ClientConfigMap;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/*
 * OAuth2 clients service
 */

@Service
public class OAuth2ClientAppService implements ClientAppService {

    private final OAuth2ClientService clientService;

    public OAuth2ClientAppService(OAuth2ClientService clientService) {
        Assert.notNull(clientService, "client service is mandatory");
        this.clientService = clientService;
    }

    @Override
    public Collection<ClientApp> listClients(String realm) {
        List<OAuth2Client> clients = clientService.listClients(realm);

        // reset credentials, accessible only if single fetch
        clients
            .stream()
            .forEach(c -> {
                c.setClientSecret(null);
                c.setClientJwks(null);
            });

        return clients.stream().map(c -> toApp(c)).collect(Collectors.toList());
    }

    public Page<ClientApp> searchClients(String realm, String keywords, Pageable pageRequest) {
        Page<OAuth2Client> page = clientService.searchClients(realm, keywords, pageRequest);

        // reset credentials, accessible only if single fetch
        page
            .getContent()
            .stream()
            .forEach(c -> {
                c.setClientSecret(null);
                c.setClientJwks(null);
            });

        return PageableExecutionUtils.getPage(
            page.getContent().stream().map(c -> toApp(c)).collect(Collectors.toList()),
            pageRequest,
            () -> page.getTotalElements()
        );
    }

    @Override
    public ClientApp findClient(String clientId) {
        OAuth2Client client = clientService.findClient(clientId);
        if (client == null) {
            return null;
        }

        return toApp(client);
    }

    @Override
    public ClientApp getClient(String clientId) throws NoSuchClientException {
        OAuth2Client client = clientService.getClient(clientId);

        return toApp(client);
    }

    @Override
    public ClientApp registerClient(String realm, String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("empty name is invalid");
        }

        OAuth2Client client = clientService.addClient(realm, name);

        return toApp(client);
    }

    @Override
    public ClientApp registerClient(String realm, ClientApp app) {
        String name = app.getName();
        String description = app.getDescription();

        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }

        if (app.getConfiguration() == null) {
            // add as new
            OAuth2Client client = clientService.addClient(
                realm,
                app.getClientId(),
                name,
                description,
                Arrays.asList(app.getScopes()),
                Arrays.asList(app.getResourceIds()),
                Arrays.asList(app.getProviders()),
                app.getHookFunctions(),
                app.getHookWebUrls(),
                app.getHookUniqueSpaces(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );

            return toApp(client);
        } else {
            // unpack
            Map<String, Serializable> configuration = app.getConfiguration();

            // convert to proper model
            OAuth2ClientConfigMap configMap = new OAuth2ClientConfigMap(configuration);

            // we don't want to set a user submitted secret
            // import anyway since we need to preserve secrets on import
            // TODO distinguish 2 flows..
            String clientSecret = configuration.containsKey("clientSecret")
                ? String.valueOf(configuration.get("clientSecret"))
                : null;
            String clientJwks = configuration.containsKey("jwks") ? String.valueOf(configuration.get("jwks")) : null;
            JWKSet jwks = null;
            if (StringUtils.hasText(clientJwks)) {
                try {
                    jwks = JWKSet.parse(clientJwks);
                } catch (ParseException e) {}
            }

            // register with autogenerated clientId
            // add as new
            OAuth2Client client = clientService.addClient(
                realm,
                app.getClientId(),
                name,
                description,
                Arrays.asList(app.getScopes()),
                Arrays.asList(app.getResourceIds()),
                Arrays.asList(app.getProviders()),
                app.getHookFunctions(),
                app.getHookWebUrls(),
                app.getHookUniqueSpaces(),
                clientSecret,
                configMap.getAuthorizedGrantTypes(),
                configMap.getRedirectUris(),
                configMap.getApplicationType(),
                configMap.getTokenType(),
                configMap.getSubjectType(),
                configMap.getAuthenticationMethods(),
                configMap.getIdTokenClaims(),
                configMap.getFirstParty(),
                configMap.getAccessTokenValidity(),
                configMap.getRefreshTokenValidity(),
                configMap.getIdTokenValidity(),
                jwks,
                configMap.getJwksUri(),
                configMap.getAdditionalConfig(),
                configMap.getAdditionalInformation()
            );

            return toApp(client);
        }
    }

    @Override
    public ClientApp updateClient(String clientId, ClientApp app) throws NoSuchClientException {
        OAuth2Client client = clientService.getClient(clientId);

        String name = app.getName();
        String description = app.getDescription();

        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }

        // unpack
        Map<String, Serializable> configuration = app.getConfiguration();

        // convert to proper model
        OAuth2ClientConfigMap configMap = new OAuth2ClientConfigMap(configuration);

        // update
        client =
            clientService.updateClient(
                clientId,
                name,
                description,
                Arrays.asList(app.getScopes()),
                Arrays.asList(app.getResourceIds()),
                Arrays.asList(app.getProviders()),
                app.getHookFunctions(),
                app.getHookWebUrls(),
                app.getHookUniqueSpaces(),
                configMap.getAuthorizedGrantTypes(),
                configMap.getRedirectUris(),
                configMap.getApplicationType(),
                configMap.getTokenType(),
                configMap.getSubjectType(),
                configMap.getAuthenticationMethods(),
                configMap.getIdTokenClaims(),
                configMap.getFirstParty(),
                configMap.getAccessTokenValidity(),
                configMap.getRefreshTokenValidity(),
                configMap.getIdTokenValidity(),
                configMap.getJwksUri(),
                configMap.getAdditionalConfig(),
                configMap.getAdditionalInformation()
            );

        return toApp(client);
    }

    @Override
    public void deleteClient(String clientId) {
        OAuth2Client client = clientService.findClient(clientId);
        if (client != null) {
            clientService.deleteClient(clientId);
        }
    }

    /*
     * helpers
     */
    private ClientApp toApp(OAuth2Client client) {
        ClientApp app = new ClientApp();
        app.setClientId(client.getClientId());
        app.setType(SystemKeys.CLIENT_TYPE_OAUTH2);

        if (StringUtils.hasText(client.getName())) {
            app.setName(client.getName());
        }
        if (StringUtils.hasText(client.getDescription())) {
            app.setDescription(client.getDescription());
        }

        app.setRealm(client.getRealm());

        app.setScopes(client.getScopes() != null ? client.getScopes().toArray(new String[0]) : null);
        app.setResourceIds(client.getResourceIds() != null ? client.getResourceIds().toArray(new String[0]) : null);
        app.setProviders(client.getProviders() != null ? client.getProviders().toArray(new String[0]) : null);
        if (client.getHookFunctions() != null) {
            app.setHookFunctions(client.getHookFunctions());
        }
        if (client.getHookWebUrls() != null) {
            app.setHookWebUrls(client.getHookWebUrls());
        }
        app.setHookUniqueSpaces(client.getHookUniqueSpaces());

        // flatten configuration
        app.setConfiguration(client.getConfiguration());

        return app;
    }

    @Override
    public JsonSchema getConfigurationSchema() {
        try {
            return OAuth2ClientConfigMap.getConfigurationSchema();
        } catch (JsonMappingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
