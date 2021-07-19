package it.smartcommunitylab.aac.apim;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.oauth.client.OAuth2Client;
import it.smartcommunitylab.aac.oauth.client.OAuth2ClientConfigMap;
import it.smartcommunitylab.aac.oauth.model.ApplicationType;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.TokenType;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientService;

/*
 * APIM manager -
 *  TODO 
 *  1. implement permission check + ownership 
 *  2. DROP and expose only oauth dynamic client registration
 */

@Component
//@Transactional
public class APIMProviderService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OAuth2ClientService clientService;

    @Autowired
    private ProviderManager providerManager;

//    @Autowired
//    private ServiceManager serviceManager;

//    @Value("${api.contextSpace}")
//    private String apiProviderContext;

    private static final String[] GRANT_TYPES = {
            Config.GRANT_TYPE_AUTHORIZATION_CODE,
            Config.GRANT_TYPE_IMPLICIT,
            Config.GRANT_TYPE_CLIENT_CREDENTIALS,
//            Config.GRANT_TYPE_NATIVE,
            Config.GRANT_TYPE_PASSWORD,
            Config.GRANT_TYPE_REFRESH_TOKEN,
            Config.GRANT_TYPE_DEVICE_CODE
    };

    public APIMClient createClient(
            String realm,
            String clientId,
            String userName,
            String clientName,
            String displayName,
            String clientSecret,
            Collection<String> grantTypes,
            String[] scopes,
            String[] redirectUris) throws RuntimeException, NoSuchClientException, NoSuchRealmException {

        // always assign CLIENT_CREDENTIALS if nothing requested
        if (grantTypes == null || grantTypes.isEmpty()) {
            grantTypes = Collections.singleton(Config.GRANT_TYPE_CLIENT_CREDENTIALS);
        }

        // filter grant types and accept only those supported, apim tries to assign
        // non-standard types
        Set<String> grantedTypes = grantTypes.stream().filter(gt -> ArrayUtils.contains(GRANT_TYPES, gt))
                .collect(Collectors.toSet());

        OAuth2Client client = null;

        // check if exists by id
        if (StringUtils.hasText(clientId)) {
            client = clientService.findClient(clientId);
        }
        // check if exists by developer and name
        if (client == null && StringUtils.hasText(clientName)) {
            client = clientService.findClient(realm, clientName);
        }

        if (client == null) {
            // create
            client = clientService.addClient(realm, clientName);

//            // fetch clientSecret, if generated
//            clientSecret = client.getSecret();

        }

        // always update, we need to ensure client is valid
        clientId = client.getClientId();

        // get current config
        OAuth2ClientConfigMap configMap = client.getConfigMap();

        // fetch and add all realm providers
        List<String> providers = providerManager.getIdentityProviders(realm).stream().map(idp -> idp.getProvider())
                .collect(Collectors.toList());

        Set<AuthenticationMethod> authenticationMethods = new HashSet<>();
        authenticationMethods.add(AuthenticationMethod.CLIENT_SECRET_BASIC);
        authenticationMethods.add(AuthenticationMethod.CLIENT_SECRET_POST);

        Set<AuthorizationGrantType> authorizedGrantTypes = grantedTypes.stream()
                .map(gt -> AuthorizationGrantType.parse(gt)).collect(Collectors.toSet());

        // update
        client = clientService.updateClient(
                clientId,
                clientName, displayName,
                Arrays.asList(scopes), null,
                providers,
                null, null, null,
                authorizedGrantTypes,
                Arrays.asList(redirectUris),
                ApplicationType.WEB, TokenType.JWT, null,
                authenticationMethods,
                false, false,
                null, null,
                null,
                null, null, null,
                null);

        return APIMClient.from(client);
    }

    public APIMClient updateClient(
            String realm,
            String clientId,
            String clientName,
            String displayName,
            String clientSecret,
            Collection<String> grantTypes,
            String[] scope,
            String[] redirectUris)
            throws EntityNotFoundException, RuntimeException, NoSuchClientException, NoSuchRealmException {

        // check if exists by id
        OAuth2Client client = clientService.findClient(clientId);
        if (client == null || !client.getRealm().equals(realm)) {
            // error
            throw new EntityNotFoundException("no client with id " + clientId);
        }

        // fetch and add all realm providers
        List<String> providers = providerManager.getIdentityProviders(realm).stream().map(idp -> idp.getProvider())
                .collect(Collectors.toList());

        // get current config
        OAuth2ClientConfigMap configMap = client.getConfigMap();

        // always assign CLIENT_CREDENTIALS if nothing requested
        if (grantTypes == null || grantTypes.isEmpty()) {
            grantTypes = Collections.singleton(Config.GRANT_TYPE_CLIENT_CREDENTIALS);
        }

        // filter grant types and accept only those supported, apim tries to assign
        // non-standard types
        // NOTE: if APIm does not provide these values we will reset them to null
        Set<String> grantedTypes = grantTypes.stream().filter(gt -> ArrayUtils.contains(GRANT_TYPES, gt))
                .collect(Collectors.toSet());

        Set<AuthorizationGrantType> authorizedGrantTypes = grantedTypes.stream()
                .map(gt -> AuthorizationGrantType.parse(gt)).collect(Collectors.toSet());

        // TODO check, looks like apim wants to use custom method for scopes
        Set<String> scopes = client.getScopes();
        if (scope != null) {
            scopes = new HashSet<>(Arrays.asList((scope)));
        }
        if (redirectUris != null) {
            configMap.setRedirectUris(new HashSet<>(Arrays.asList((redirectUris))));
        }

        // update
        client = clientService.updateClient(clientId,
                clientName, displayName,
                scopes, client.getResourceIds(),
                providers,
                client.getHookFunctions(), client.getHookWebUrls(), client.getHookUniqueSpaces(),
                authorizedGrantTypes, configMap.getRedirectUris(),
                configMap.getApplicationType(), configMap.getTokenType(), configMap.getSubjectType(),
                configMap.getAuthenticationMethods(),
                configMap.getIdTokenClaims(), configMap.getFirstParty(),
                configMap.getAccessTokenValidity(), configMap.getRefreshTokenValidity(),
                configMap.getIdTokenValidity(),
                configMap.getJwks(), configMap.getJwksUri(),
                configMap.getAdditionalConfig(),
                configMap.getAdditionalInformation());

        return APIMClient.from(client);
    }

    public APIMClient updateValidity(String realm, String clientId, Integer validity)
            throws EntityNotFoundException, RuntimeException, NoSuchClientException {

        // check if exists by id
        OAuth2Client client = clientService.findClient(clientId);
        if (client == null || !client.getRealm().equals(realm)) {
            // error
            throw new EntityNotFoundException("no client with id " + clientId);
        }

        // get current config
        OAuth2ClientConfigMap configMap = client.getConfigMap();

        // update
        client = clientService.updateClient(clientId,
                client.getName(), client.getDescription(),
                client.getScopes(), client.getResourceIds(),
                client.getProviders(),
                client.getHookFunctions(), client.getHookWebUrls(), client.getHookUniqueSpaces(),
                configMap.getAuthorizedGrantTypes(), configMap.getRedirectUris(),
                configMap.getApplicationType(), configMap.getTokenType(), configMap.getSubjectType(),
                configMap.getAuthenticationMethods(),
                configMap.getIdTokenClaims(), configMap.getFirstParty(),
                validity, configMap.getRefreshTokenValidity(),
                validity,
                configMap.getJwks(), configMap.getJwksUri(),
                configMap.getAdditionalConfig(),
                configMap.getAdditionalInformation());

        return APIMClient.from(client);

    }

    public APIMClient updateScope(String realm, String clientId, String scope)
            throws EntityNotFoundException, RuntimeException, NoSuchClientException {

        // check if exists by id
        OAuth2Client client = clientService.findClient(clientId);
        if (client == null || !client.getRealm().equals(realm)) {
            // error
            throw new EntityNotFoundException("no client with id " + clientId);
        }

        String[] scopes = scope != null ? scope.split(APIMClient.SEPARATOR) : null;

        // get current config
        OAuth2ClientConfigMap configMap = client.getConfigMap();

        // update
        client = clientService.updateClient(clientId,
                client.getName(), client.getDescription(),
                Arrays.asList(scopes), client.getResourceIds(),
                client.getProviders(),
                client.getHookFunctions(), client.getHookWebUrls(), client.getHookUniqueSpaces(),
                configMap.getAuthorizedGrantTypes(), configMap.getRedirectUris(),
                configMap.getApplicationType(), configMap.getTokenType(), configMap.getSubjectType(),
                configMap.getAuthenticationMethods(),
                configMap.getIdTokenClaims(),
                configMap.getFirstParty(),
                configMap.getAccessTokenValidity(), configMap.getRefreshTokenValidity(),
                configMap.getIdTokenValidity(),
                configMap.getJwks(), configMap.getJwksUri(),
                configMap.getAdditionalConfig(),
                configMap.getAdditionalInformation());

        return APIMClient.from(client);

    }

    public APIMClient getClient(String realm, String clientId) throws EntityNotFoundException, RuntimeException {
        OAuth2Client client = clientService.findClient(clientId);
        if (client == null || !client.getRealm().equals(realm)) {
            // error
            throw new EntityNotFoundException("no client with id " + clientId);
        }

        return APIMClient.from(client);

    }

    public void deleteClient(String realm, String clientId) throws EntityNotFoundException, RuntimeException {
        // TODO check if client is owned!
        OAuth2Client client = clientService.findClient(clientId);
        if (client == null || !client.getRealm().equals(realm)) {
            // error
            throw new EntityNotFoundException("no client with id " + clientId);
        }

        clientService.deleteClient(clientId);
    }

    // DEPRECATED, dangerous direct access to repository!
//    public APIMClient createClient(APIMClient app, String userName) throws Exception {
//        User user = userRepository.findByUsername(userName);
//
//        if (user == null) {
//            return null;
//        }
//
//        ClientAppBasic resApp = clientDetailsManager.createOrUpdate(app, user.getId());
//        resApp.setRedirectUris(app.getRedirectUris());
//        resApp.setGrantedTypes(app.getGrantedTypes());
//        clientDetailsManager.update(resApp.getClientId(), resApp);
//
//        return resApp;
//    }
//
//    public APIMClient updateClient(String clientId, APIMClient app) throws Exception {
//        ClientAppBasic resApp = clientDetailsManager.update(clientId, app);
//
//        return resApp;
//    }
//
//    public void updateValidity(String clientId, Integer validity) throws Exception {
//        ClientDetailsEntity entity = clientDetailsRepository.findByClientId(clientId);
//
//        entity.setAccessTokenValidity(validity);
////		entity.setRefreshTokenValidity(validity);
//
//        clientDetailsRepository.save(entity);
//    }
//
//    public void updateClientScope(String clientId, String scope) throws Exception {
//        ClientDetailsEntity entity = clientDetailsRepository.findByClientId(clientId);
//        Set<String> oldScope = entity.getScope();
//        oldScope.addAll(Splitter.on(",").splitToList(scope));
//        entity.setScope(Joiner.on(",").join(oldScope));
//
//        Set<String> serviceIds = serviceManager.findServiceIdsByScopes(entity.getScope());
//        entity.setResourceIds(StringUtils.collectionToCommaDelimitedString(serviceIds));
//
//        ClientAppBasic resApp = clientDetailsManager.convertToClientApp(entity);
//        clientDetailsManager.update(entity.getClientId(), resApp);
//    }
//
//    public ClientAppBasic getClient(String consumerKey) throws Exception {
//        ClientDetailsEntity entity = clientDetailsRepository.findByClientId(consumerKey);
//        ClientAppBasic resApp = clientDetailsManager.convertToClientApp(entity);
//
//        return resApp;
//    }
//
//    public void deleteClient(String clientId) throws Exception {
//        ClientDetailsEntity entity = clientDetailsRepository.findByClientId(clientId);
//        clientDetailsRepository.delete(entity);
//    }

    public boolean createResource(AACService service, String userName, String tenant) throws Exception {
        // Do nothing: scopes / services should already exist
        return true;
    }

    public void deleteResource(String resourceName) throws Exception {
        // Do nothing: scopes / services should already exist
    }

}
