package it.smartcommunitylab.aac.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.model.Client;
import it.smartcommunitylab.aac.core.model.ClientCredentials;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.persistence.ClientRoleEntity;
import it.smartcommunitylab.aac.core.persistence.ProviderEntity;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.core.service.ProviderService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.oauth.model.ClientSecret;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientAppService;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientService;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

@Service
public class ClientManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private ClientEntityService clientService;

    // base services
    @Autowired
    private OAuth2ClientService oauthClientService;

    // app services
    @Autowired
    private OAuth2ClientAppService oauthClientAppService;

    @Autowired
    private ScopeRegistry scopeRegistry;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private RealmService realmService;

    @Autowired
    private SearchableApprovalStore approvalStore;

    @Autowired
    private ProviderService providerService;

    /*
     * ClientApp via appService
     * 
     * TODO add permission checkers
     */

    @Transactional(readOnly = true)
    public Collection<ClientApp> listClientApps(String realm) throws NoSuchRealmException {
        logger.debug("list client apps for realm " + realm);

        Realm r = realmService.getRealm(realm);
        List<ClientApp> apps = new ArrayList<>();

        // we support only oauth for now
        apps.addAll(listClientApps(r.getSlug(), SystemKeys.CLIENT_TYPE_OAUTH2));

        return apps;

    }

    @Transactional(readOnly = true)
    public Collection<ClientApp> listClientApps(String realm, String type) throws NoSuchRealmException {
        logger.debug("list client apps for realm " + realm + " type " + type);

        Realm r = realmService.getRealm(realm);
        if (SystemKeys.CLIENT_TYPE_OAUTH2.equals(type)) {
            return oauthClientAppService.listClients(r.getSlug());
        }

        throw new IllegalArgumentException("invalid client type");
    }

    @Deprecated
    @Transactional(readOnly = true)
    public ClientApp findClientApp(String realm, String clientId) {
        logger.debug("find client app " + String.valueOf(clientId) + " for realm " + realm);

        // get type by loading base client
        // TODO optimize to avoid db fetch
        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            return null;
        }

        String type = entity.getType();
        ClientApp clientApp = null;

        if (SystemKeys.CLIENT_TYPE_OAUTH2.equals(type)) {
            clientApp = oauthClientAppService.findClient(clientId);
        }

        // check realm match
        if (!clientApp.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm mismatch");
        }

        return clientApp;
    }

    @Transactional(readOnly = true)
    public ClientApp getClientApp(String realm, String clientId) throws NoSuchClientException, NoSuchRealmException {
        logger.debug("get client app " + String.valueOf(clientId) + " for realm " + realm);

        Realm r = realmService.getRealm(realm);

        // get type by loading base client
        // TODO optimize to avoid db fetch
        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }

        String type = entity.getType();
        ClientApp clientApp = null;

        if (SystemKeys.CLIENT_TYPE_OAUTH2.equals(type)) {
            clientApp = oauthClientAppService.getClient(clientId);
        }

        if (clientApp == null) {
            throw new IllegalArgumentException("invalid client type");
        }

        // check realm match
        if (!clientApp.getRealm().equals(r.getSlug())) {
            throw new AccessDeniedException("realm mismatch");
        }

        return clientApp;
    }

//    public ClientApp registerClientApp(String realm, String type, String name) {
//        if (SystemKeys.CLIENT_TYPE_OAUTH2.equals(type)) {
//            return oauthClientAppService.registerClient(realm, name);
//        }
//
//        throw new IllegalArgumentException("invalid client type");
//
//    }

    @Transactional(readOnly = false)
    public ClientApp registerClientApp(String realm, ClientApp app) throws NoSuchRealmException {
        logger.debug("register client app for realm " + realm);

        Realm r = realmService.getRealm(realm);

        String type = app.getType();

        // providers
        if (app.getProviders() == null || app.getProviders().length == 0) {
            // enable all registered providers by default
            List<ProviderEntity> providers = providerService.listProvidersByRealmAndType(realm,
                    SystemKeys.RESOURCE_IDENTITY);
            Set<String> providerIds = providers.stream()
                    .map(p -> p.getProviderId())
                    .collect(Collectors.toSet());
            app.setProviders(providerIds.toArray(new String[0]));
        }

        // scopes
        Set<String> appScopes = new HashSet<>(Arrays.asList(app.getScopes()));
        Set<String> invalidScopes = appScopes.stream().filter(s -> scopeRegistry.findScope(s) == null)
                .collect(Collectors.toSet());
        if (!invalidScopes.isEmpty()) {
            throw new IllegalArgumentException("invalid scopes: " + invalidScopes.toString());
        }

        // config
        if (SystemKeys.CLIENT_TYPE_OAUTH2.equals(type)) {
            app.setRealm(realm);

            if (logger.isTraceEnabled()) {
                logger.trace("app :" + String.valueOf(app));
            }

            return oauthClientAppService.registerClient(r.getSlug(), app);
        }

        throw new IllegalArgumentException("invalid client type");

    }

    @Transactional(readOnly = false)
    public ClientApp updateClientApp(String realm, String clientId, ClientApp app)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("update client app " + String.valueOf(clientId) + " for realm " + realm);

        Realm r = realmService.getRealm(realm);
        String type = app.getType();
        ClientApp clientApp = null;

        Set<String> appScopes = new HashSet<>(Arrays.asList(app.getScopes()));
        Set<String> invalidScopes = appScopes.stream().filter(s -> scopeRegistry.findScope(s) == null)
                .collect(Collectors.toSet());
        if (!invalidScopes.isEmpty()) {
            throw new IllegalArgumentException("invalid scopes: " + invalidScopes.toString());
        }

        if (SystemKeys.CLIENT_TYPE_OAUTH2.equals(type)) {
            clientApp = oauthClientAppService.getClient(clientId);

            // check realm match
            if (!clientApp.getRealm().equals(r.getSlug())) {
                throw new AccessDeniedException("realm mismatch");
            }

            if (logger.isTraceEnabled()) {
                logger.trace("app :" + String.valueOf(app));
            }

            clientApp = oauthClientAppService.updateClient(clientId, app);
        }

        if (clientApp == null) {
            throw new IllegalArgumentException("invalid client type");
        }

        return clientApp;
    }

    @Transactional(readOnly = false)
    public void deleteClientApp(String realm, String clientId) throws NoSuchClientException, NoSuchRealmException {
        logger.debug("delete client app " + String.valueOf(clientId) + " for realm " + realm);

        Realm r = realmService.getRealm(realm);

        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }

        // check realm match
        if (!entity.getRealm().equals(r.getSlug())) {
            throw new AccessDeniedException("realm mismatch");
        }

        // delete via service to destroy related resources
        deleteClient(clientId);

    }

    /*
     * Client via service TODO move to dedicated service
     * 
     * we don't expose setters, we let appServices handle configuration
     */

//    public Client getClient(String clientId) throws NoSuchClientException {
//        ClientEntity entity = findClient(clientId);
//        if (entity == null) {
//            throw new NoSuchClientException();
//        }
//
//        String type = entity.getType();
//
//        Client client = null;
//        if (SystemKeys.CLIENT_TYPE_OAUTH2.equals(type)) {
//            client = oauthClientService.getClient(clientId);
//        }
//
//        if (client == null) {
//            throw new IllegalArgumentException("invalid client type");
//        }
//
//        return client;
//
//    }

    @Transactional(readOnly = true)
    public List<Client> listClients(String realm) throws NoSuchRealmException {
        logger.debug("list clients for realm " + realm);

        Realm r = realmService.getRealm(realm);

        List<Client> apps = new ArrayList<>();

        // we support only oauth for now
        apps.addAll(oauthClientService.listClients(r.getSlug()));

        return apps;
    }

    @Transactional(readOnly = false)
    private void deleteClient(String clientId) throws NoSuchClientException {
        logger.debug("delete client " + String.valueOf(clientId));

        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }

        String type = entity.getType();

        Client client = null;
        if (SystemKeys.CLIENT_TYPE_OAUTH2.equals(type)) {
            client = oauthClientService.getClient(clientId);
        }

        if (client == null) {
            throw new IllegalArgumentException("invalid client type");
        }

        // session invalidation
        sessionManager.destroyClientSessions(clientId);

        // TODO token revoke, cleanups etc
        // most things should be handled by the downstream service

        // remove approvals
        try {
            Collection<Approval> approvals = approvalStore.findClientApprovals(clientId);
            approvalStore.revokeApprovals(approvals);
        } catch (Exception e) {
        }

        // delete
        if (SystemKeys.CLIENT_TYPE_OAUTH2.equals(type)) {
            oauthClientService.deleteClient(clientId);
        } else {
            throw new IllegalArgumentException("invalid client type");
        }

    }

    /*
     * Client Credentials via service
     */

    @Transactional(readOnly = true)
    public ClientCredentials getClientCredentials(String realm, String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("get credentials for client " + String.valueOf(clientId) + " for realm " + realm);

        Realm r = realmService.getRealm(realm);

        // get type by loading base client
        // TODO optimize to avoid db fetch
        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }

        // check realm match
        if (!entity.getRealm().equals(r.getSlug())) {
            throw new AccessDeniedException("realm mismatch");
        }

        String type = entity.getType();

        if (SystemKeys.CLIENT_TYPE_OAUTH2.equals(type)) {
            return oauthClientService.getClientCredentials(clientId);
        }

        throw new IllegalArgumentException("invalid client type");
    }

    @Transactional(readOnly = false)
    public ClientCredentials resetClientCredentials(String realm, String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("reset credentials for client " + String.valueOf(clientId) + " for realm " + realm);

        Realm r = realmService.getRealm(realm);

        // get type by loading base client
        // TODO optimize to avoid db fetch (better in service)
        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }

        // check realm match
        if (!entity.getRealm().equals(r.getSlug())) {
            throw new AccessDeniedException("realm mismatch");
        }

        String type = entity.getType();

        if (SystemKeys.CLIENT_TYPE_OAUTH2.equals(type)) {
            return oauthClientService.resetClientCredentials(clientId);
        }

        throw new IllegalArgumentException("invalid client type");
    }

    @Transactional(readOnly = false)
    public ClientCredentials setClientCredentials(String realm, String clientId, Map<String, Object> credentials)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("set credentials for client " + String.valueOf(clientId) + " for realm " + realm);

        Realm r = realmService.getRealm(realm);

        // get type by loading base client
        // TODO optimize to avoid db fetch
        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }

        // check realm match
        if (!entity.getRealm().equals(r.getSlug())) {
            throw new AccessDeniedException("realm mismatch");
        }

        String type = entity.getType();

        if (SystemKeys.CLIENT_TYPE_OAUTH2.equals(type)) {
            // convert to credentials
            ClientSecret secret = mapper.convertValue(credentials, ClientSecret.class);
            return oauthClientService.setClientCredentials(clientId, secret);
        }

        throw new IllegalArgumentException("invalid client type");
    }

    /*
     * Client roles
     */

    @Transactional(readOnly = true)
    public Collection<RealmRole> getRoles(String realm, String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("get roles for client " + String.valueOf(clientId) + " in realm " + realm);

        Realm rlm = realmService.getRealm(realm);

        // TODO optimize to avoid db fetch
        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }

        List<ClientRoleEntity> clientRoles = clientService.getRoles(clientId, rlm.getSlug());
        Set<RealmRole> realmRoles = clientRoles.stream()
                .map(r -> new RealmRole(r.getRealm(), r.getRole()))
                .collect(Collectors.toSet());

        return realmRoles;

    }

    @Transactional(readOnly = false)
    public Collection<RealmRole> updateRoles(String clientId, String realm, Collection<String> roles)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("update roles for client " + String.valueOf(clientId) + " in realm " + realm);

        Realm rlm = realmService.getRealm(realm);

        // TODO optimize to avoid db fetch
        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("roles: " + String.valueOf(roles));
        }

        List<ClientRoleEntity> clientRoles = clientService.updateRoles(clientId, rlm.getSlug(), roles);
        Set<RealmRole> realmRoles = clientRoles.stream()
                .map(r -> new RealmRole(r.getRealm(), r.getRole()))
                .collect(Collectors.toSet());

        return realmRoles;
    }

    // TODO evaluate removal, no reason to expose all roles
    @Transactional(readOnly = false)
    protected Collection<RealmRole> getRoles(String clientId) throws NoSuchClientException {
        logger.debug("get roles for client " + String.valueOf(clientId));

        // TODO optimize to avoid db fetch
        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }

        List<ClientRoleEntity> clientRoles = clientService.getRoles(clientId);
        Set<RealmRole> realmRoles = clientRoles.stream()
                .map(r -> new RealmRole(r.getRealm(), r.getRole()))
                .collect(Collectors.toSet());

        return realmRoles;

    }

    /*
     * Configuration schemas
     */

    public JsonSchema getConfigurationSchema(String type) {
        logger.debug("get config schema for client type " + type);

        if (SystemKeys.CLIENT_TYPE_OAUTH2.equals(type)) {
            return oauthClientAppService.getConfigurationSchema();
        }

        throw new IllegalArgumentException("invalid client type");
    }

    /*
     * Base client
     */
    private ClientEntity findClient(String clientId) {
        return clientService.findClient(clientId);
    }

}
