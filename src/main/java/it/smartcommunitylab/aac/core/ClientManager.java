package it.smartcommunitylab.aac.core;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.audit.store.AuditEventStore;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.model.Client;
import it.smartcommunitylab.aac.core.model.ClientCredentials;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientAppService;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientService;
import it.smartcommunitylab.aac.oauth.store.ExtTokenStore;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.roles.model.RealmRole;
import it.smartcommunitylab.aac.roles.service.SpaceRoleService;
import it.smartcommunitylab.aac.roles.service.SubjectRoleService;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_DEVELOPER + "')")
public class ClientManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

//    private static ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private ClientEntityService clientService;

    // base services
    @Autowired
    private OAuth2ClientService oauthClientService;

    // app services
    @Autowired
    private OAuth2ClientAppService oauthClientAppService;

    @Autowired
    private SpaceRoleService spaceRoleService;

    @Autowired
    private SubjectRoleService subjectRoleService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private ScopeRegistry scopeRegistry;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private RealmService realmService;

    @Autowired
    private SearchableApprovalStore approvalStore;

    @Autowired
    private IdentityProviderService providerService;

    @Autowired
    private SubjectRoleService realmRoleService;

    @Autowired
    private ExtTokenStore tokenStore;

    @Autowired
    private AuditEventStore auditStore;

    /*
     * ClientApp via appService
     * 
     * TODO add permission checkers
     */
    @Transactional(readOnly = true)
    public Collection<ClientApp> listClientApps(String realm) throws NoSuchRealmException {
        logger.debug("list client apps for realm {}", StringUtils.trimAllWhitespace(realm));

        Realm r = realmService.getRealm(realm);
        List<ClientApp> apps = new ArrayList<>();

        // we support only oauth for now
        apps.addAll(listClientApps(r.getSlug(), SystemKeys.CLIENT_TYPE_OAUTH2));

        return apps;

    }

    @Transactional(readOnly = true)
    public Collection<ClientApp> listClientApps(String realm, String type) throws NoSuchRealmException {
        logger.debug("list client apps for realm {} type {}", StringUtils.trimAllWhitespace(realm),
                StringUtils.trimAllWhitespace(type));

        Realm r = realmService.getRealm(realm);
        Collection<ClientApp> apps = Collections.emptyList();
        if (SystemKeys.CLIENT_TYPE_OAUTH2.equals(type)) {
            apps = oauthClientAppService.listClients(r.getSlug());
            // load realm roles
            apps.stream().forEach(app -> {
                try {
                    // load realm roles
                    Collection<RealmRole> roles = loadClientRoles(realm, app.getClientId());
                    app.setRealmRoles(roles);

                    // load authorities
                    Collection<RealmGrantedAuthority> authorities = loadClientAuthorities(realm, app.getClientId());
                    app.setAuthorities(authorities);
                } catch (NoSuchClientException e) {
                }
            });

            return apps;
        }

        throw new IllegalArgumentException("invalid client type");
    }

//    @Transactional(readOnly = true)
//    public long countClientApps(String realm) throws NoSuchRealmException {
//        logger.debug("count client apps for realm " + realm);
//
//        Realm r = realmService.getRealm(realm);
//        // we support only oauth for now
//        return oauthClientAppService.countClients(r.getSlug());
//
//    }

    @Transactional(readOnly = true)
    public Page<ClientApp> searchClientApps(String realm, String keywords, Pageable pageRequest)
            throws NoSuchRealmException {
        logger.debug("search clients for realm {} with keywords {}", StringUtils.trimAllWhitespace(realm),
                StringUtils.trimAllWhitespace(keywords));

        String query = StringUtils.trimAllWhitespace(keywords);
        Realm r = realmService.getRealm(realm);
        // we support only oauth for now
        Page<ClientApp> page = oauthClientAppService.searchClients(r.getSlug(), query, pageRequest);
        // load realm roles
        page.get().forEach(clientApp -> {
            try {
                // load realm roles
                Collection<RealmRole> roles = loadClientRoles(realm, clientApp.getClientId());
                clientApp.setRealmRoles(roles);

                // load authorities
                Collection<RealmGrantedAuthority> authorities = loadClientAuthorities(realm, clientApp.getClientId());
                clientApp.setAuthorities(authorities);
            } catch (NoSuchClientException e) {
            }
        });

        return page;
    }

    @Deprecated
    @Transactional(readOnly = true)
    public ClientApp findClientApp(String realm, String clientId) {
        logger.debug("find client app {} for realm {}", StringUtils.trimAllWhitespace(clientId),
                StringUtils.trimAllWhitespace(realm));

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

        if (clientApp == null) {
            return null;
        }

        // check realm match
        if (!clientApp.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm mismatch");
        }

        try {
            // load realm roles
            Collection<RealmRole> roles = loadClientRoles(realm, clientApp.getClientId());
            clientApp.setRealmRoles(roles);
        } catch (NoSuchClientException e) {
        }

        return clientApp;
    }

    @Transactional(readOnly = true)
    public ClientApp getClientApp(String realm, String clientId) throws NoSuchClientException, NoSuchRealmException {
        logger.debug("get client app {}  for realm {}", StringUtils.trimAllWhitespace(clientId),
                StringUtils.trimAllWhitespace(realm));

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

        // load realm roles
        Collection<RealmRole> roles = loadClientRoles(realm, clientApp.getClientId());
        clientApp.setRealmRoles(roles);

        Collection<SpaceRole> spaceRoles = loadClientSpaceRoles(realm, clientApp.getClientId());
        clientApp.setSpaceRoles(spaceRoles);

        clientApp.setAuthorities(getAuthorities(realm, clientApp.getClientId()));

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
        logger.debug("register client app for realm {}", StringUtils.trimAllWhitespace(realm));

        Realm r = realmService.getRealm(realm);

        String type = app.getType();

        // providers
        if (app.getProviders() == null || app.getProviders().length == 0) {
            // enable all registered providers by default
            Collection<ConfigurableIdentityProvider> providers = providerService.listProviders(realm);
            Set<String> providerIds = providers.stream()
                    .map(p -> p.getProvider())
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
                logger.trace("app: {}", StringUtils.trimAllWhitespace(String.valueOf(app)));
            }

            ClientApp clientApp = oauthClientAppService.registerClient(r.getSlug(), app);
            try {
                // load realm roles
                Collection<RealmRole> roles = loadClientRoles(realm, clientApp.getClientId());
                clientApp.setRealmRoles(roles);

            } catch (NoSuchClientException e) {
            }

            return clientApp;
        }

        throw new IllegalArgumentException("invalid client type");

    }

    @Transactional(readOnly = false)
    public ClientApp updateClientApp(String realm, String clientId, ClientApp app)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("update client app {}  for realm {}", StringUtils.trimAllWhitespace(clientId),
                StringUtils.trimAllWhitespace(realm));

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
                logger.trace("app: {}", String.valueOf(app));
            }

            clientApp = oauthClientAppService.updateClient(clientId, app);
        }

        if (clientApp == null) {
            throw new IllegalArgumentException("invalid client type");
        }

        // load realm roles
        Collection<RealmRole> roles = loadClientRoles(realm, clientApp.getClientId());
        clientApp.setRealmRoles(roles);

        return clientApp;
    }

    @Transactional(readOnly = false)
    public void deleteClientApp(String realm, String clientId) throws NoSuchClientException, NoSuchRealmException {
        logger.debug("delete client app {} for realm {}", StringUtils.trimAllWhitespace(clientId),
                StringUtils.trimAllWhitespace(realm));

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
        logger.debug("list clients for realm {}", StringUtils.trimAllWhitespace(realm));

        Realm r = realmService.getRealm(realm);

        List<Client> apps = new ArrayList<>();

        // we support only oauth for now
        apps.addAll(oauthClientService.listClients(r.getSlug()));

        return apps;
    }

    /*
     * Client Credentials via service
     */
    @Transactional(readOnly = true)
    public Collection<ClientCredentials> getClientCredentials(String realm, String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("get credentials for client {} for realm {}", StringUtils.trimAllWhitespace(clientId),
                StringUtils.trimAllWhitespace(realm));

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

    @Transactional(readOnly = true)
    public ClientCredentials getClientCredentials(String realm, String clientId, String credentialsId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("get credentials for client {} for realm {}", StringUtils.trimAllWhitespace(clientId),
                StringUtils.trimAllWhitespace(realm));

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
            return oauthClientService.getClientCredentials(clientId, credentialsId);
        }

        throw new IllegalArgumentException("invalid client type");
    }

    @Transactional(readOnly = false)
    public ClientCredentials resetClientCredentials(String realm, String clientId, String credentialsId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("reset credentials {} for client {} for realm {}", StringUtils.trimAllWhitespace(credentialsId),
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(realm));

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
            return oauthClientService.resetClientCredentials(clientId, credentialsId);
        }

        throw new IllegalArgumentException("invalid client type");
    }

    @Transactional(readOnly = false)
    public void removeClientCredentials(String realm, String clientId, String credentialsId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("remove credentials {} for client {} for realm {}", StringUtils.trimAllWhitespace(credentialsId),
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(realm));

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
            oauthClientService.removeClientCredentials(clientId, credentialsId);
        } else {
            throw new IllegalArgumentException("invalid client type");
        }
    }

    @Transactional(readOnly = false)
    public ClientCredentials setClientCredentials(String realm, String clientId, String credentialsId,
            ClientCredentials credentials)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("set credentials {} for client {} for realm {}", StringUtils.trimAllWhitespace(credentialsId),
                StringUtils.trimAllWhitespace(clientId), StringUtils.trimAllWhitespace(realm));

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
            return oauthClientService.setClientCredentials(clientId, credentialsId, credentials);
        }

        throw new IllegalArgumentException("invalid client type");
    }

    /*
     * Client realm roles
     * 
     * TODO evaluate removal from manager
     */

    @Transactional(readOnly = true)
    public Collection<RealmRole> getRoles(String realm, String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("get roles for client {} in realm {}", StringUtils.trimAllWhitespace(clientId),
                StringUtils.trimAllWhitespace(realm));

        Realm r = realmService.getRealm(realm);

        // TODO optimize to avoid db fetch
        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }

        Collection<RealmRole> realmRoles = subjectRoleService.getRoles(clientId, r.getSlug());

        return realmRoles;

    }

    @Transactional(readOnly = false)
    public Collection<RealmRole> updateRoles(String realm, String clientId, Collection<String> roles)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("update roles for client {} in realm {}", StringUtils.trimAllWhitespace(clientId),
                StringUtils.trimAllWhitespace(realm));

        Realm r = realmService.getRealm(realm);

        // TODO optimize to avoid db fetch
        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("roles: {}", String.valueOf(roles));
        }

        Collection<RealmRole> realmRoles = subjectRoleService.setRoles(clientId, r.getSlug(), roles);

        return realmRoles;
    }

    /**
     * Client authorities
     * 
     * do note access should be restricted to ADMIN
     */
    @Transactional(readOnly = true)
    public Collection<RealmGrantedAuthority> getAuthorities(
            String realm, String clientId) throws NoSuchRealmException, NoSuchClientException {
        logger.debug("get authorities for app {} in realm {}", StringUtils.trimAllWhitespace(clientId),
                StringUtils.trimAllWhitespace(realm));

        Realm r = realmService.getRealm(realm);
        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }

        return loadClientAuthorities(r.getSlug(), clientId);
    }

    @Transactional(readOnly = false)
    public Collection<GrantedAuthority> setAuthorities(String realm, String clientId, Collection<String> roles)
            throws NoSuchRealmException, NoSuchClientException {
        logger.debug("update authorities for app {} in realm {}", StringUtils.trimAllWhitespace(clientId),
                StringUtils.trimAllWhitespace(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("authorities: {}", String.valueOf(roles));
        }

        Realm r = realmService.getRealm(realm);
        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }
        try {
            return subjectService.updateAuthorities(clientId, r.getSlug(), roles);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchClientException();
        }
    }

    public Collection<Approval> getApprovals(String realm, String clientId) throws NoSuchClientException {
        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }

        Collection<Approval> approvals = approvalStore.findClientApprovals(clientId);
        return approvals;
    }

    public Collection<OAuth2AccessToken> getAccessTokens(String realm, String clientId) throws NoSuchClientException {
        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }

        Collection<OAuth2AccessToken> tokens = tokenStore.findTokensByClientId(clientId);
        return tokens;
    }

    public Collection<AuditEvent> getAudit(String realm, String clientId, Date after, Date before)
            throws NoSuchClientException {
        ClientEntity entity = findClient(clientId);
        if (entity == null) {
            throw new NoSuchClientException();
        }

        Instant now = Instant.now();
        Instant a = after == null ? now.minus(5, ChronoUnit.DAYS) : after.toInstant();
        Instant b = before == null ? now : before.toInstant();

        return auditStore.findByPrincipal(clientId, a, b, null);
    }

//    // TODO evaluate removal, no reason to expose all roles
//    @Transactional(readOnly = false)
//    protected Collection<RealmRole> getRoles(String clientId) throws NoSuchClientException {
//        logger.debug("get roles for client " + String.valueOf(clientId));
//
//        // TODO optimize to avoid db fetch
//        ClientEntity entity = findClient(clientId);
//        if (entity == null) {
//            throw new NoSuchClientException();
//        }
//
//        List<ClientRoleEntity> clientRoles = clientService.getRoles(clientId);
//        Set<RealmRole> realmRoles = clientRoles.stream()
//                .map(r -> new RealmRole(r.getRealm(), r.getRole()))
//                .collect(Collectors.toSet());
//
//        return realmRoles;
//
//    }

    /*
     * Configuration schemas
     */
    @Transactional(readOnly = true)
    public JsonSchema getClientConfigurationSchema(String realm, String clientId)
            throws NoSuchClientException, NoSuchRealmException {
        logger.debug("get configuration schema for client {} for realm {}", StringUtils.trimAllWhitespace(clientId),
                StringUtils.trimAllWhitespace(realm));

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
            return oauthClientAppService.getConfigurationSchema();
        }

        throw new IllegalArgumentException("invalid client type");
    }

    /*
     * Identity providers
     * 
     * workaround TODO split client+provider registration out as dedicated
     * entity+service
     */
    public Collection<ConfigurableIdentityProvider> listIdentityProviders(String realm) throws NoSuchRealmException {
        Realm re = realmService.getRealm(realm);
        return providerService.listProviders(re.getSlug()).stream()
                .map(cp -> {
                    // clear config and reserved info
                    cp.setEvents(null);
                    cp.setPersistence(null);
                    cp.setSchema(null);
                    cp.setConfiguration(null);
                    cp.setHookFunctions(null);

                    return cp;
                }).collect(Collectors.toList());
    }

    /*
     * Base client
     */
    private ClientEntity findClient(String clientId) {
        return clientService.findClient(clientId);
    }

    @Transactional(readOnly = false)
    private void deleteClient(String clientId) throws NoSuchClientException {
        logger.debug("delete client {}", StringUtils.trimAllWhitespace(clientId));

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

    private Collection<RealmRole> loadClientRoles(String realm, String clientId) throws NoSuchClientException {
        return realmRoleService.getRoles(clientId, realm);
//        List<ClientRoleEntity> clientRoles = clientService.getRoles(clientId, realm);
//        return clientRoles.stream()
//                .map(r -> new RealmRole(r.getRealm(), r.getRole()))
//                .collect(Collectors.toSet());
    }

    private Collection<SpaceRole> loadClientSpaceRoles(String realm, String clientId) throws NoSuchClientException {
        return spaceRoleService.getRoles(clientId);
    }

    private Collection<RealmGrantedAuthority> loadClientAuthorities(String realm, String clientId)
            throws NoSuchClientException {
        return subjectService.getAuthorities(clientId, realm).stream()
                .filter(a -> a instanceof RealmGrantedAuthority).map(a -> (RealmGrantedAuthority) a)
                .collect(Collectors.toList());
    }

}
