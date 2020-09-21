package it.smartcommunitylab.aac.apim;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import javax.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.manager.ClientDetailsManager;
import it.smartcommunitylab.aac.manager.ServiceManager;
import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.UserRepository;

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
    private ClientDetailsManager clientManager;

    @Autowired
    private UserRepository userRepository;

//    @Autowired
//    private ServiceManager serviceManager;

//    @Value("${api.contextSpace}")
//    private String apiProviderContext;

    public APIMClient createClient(
            String clientId,
            String userName,
            String clientName,
            String displayName,
            String clientSecret,
            Collection<String> grantTypes,
            String[] scopes,
            String[] redirectUris) throws RuntimeException {
        User user = userRepository.findByUsername(userName);

        if (user == null) {
            return null;
        }

        Long userId = user.getId();
        ClientAppBasic client = null;

        // always assign CLIENT_CREDENTIALS if nothing requested
        if (grantTypes == null || grantTypes.isEmpty()) {
            grantTypes = Collections.singleton(Config.GRANT_TYPE_CLIENT_CREDENTIALS);
        }

        // check if exists by id
        if (StringUtils.hasText(clientId)) {
            client = clientManager.findByClientId(clientId);
        }
        // check if exists by developer and name
        if (client == null && StringUtils.hasText(clientName)) {
            client = clientManager.findByNameAndUserId(clientName, userId);
        }

        if (client == null) {
            // create
            client = clientManager.create(clientId, userId,
                    clientName, clientSecret,
                    grantTypes.toArray(new String[0]), scopes, redirectUris);

            // fetch clientSecret, if generated
            clientSecret = client.getClientSecret();

        }

        // always update, we need to ensure client is valid
        clientId = client.getClientId();

        // update basic
        ClientAppBasic appData = client;
        appData.setName(clientName);
        appData.setDisplayName(displayName);
        appData.setGrantedTypes(new HashSet<>(grantTypes));
        appData.setScope(new HashSet<>(Arrays.asList((scopes))));
        appData.setRedirectUris(new HashSet<>(Arrays.asList((redirectUris))));

        // NOTE: APIM client need manual Idp and scope approval from AAC

        client = clientManager.update(clientId, appData, clientSecret);

        return APIMClient.from(client);
    }

    public APIMClient updateClient(
            String clientId,
            String clientName,
            String displayName,
            String clientSecret,
            Collection<String> grantTypes,
            String[] scopes,
            String[] redirectUris) throws EntityNotFoundException, RuntimeException {

        // check if exists by id
        ClientAppBasic client = clientManager.findByClientId(clientId);
        if (client == null) {
            // error
            throw new EntityNotFoundException("no client with id " + clientId);
        }

        // always assign CLIENT_CREDENTIALS if nothing requested
        if (grantTypes == null || grantTypes.isEmpty()) {
            grantTypes = Collections.singleton(Config.GRANT_TYPE_CLIENT_CREDENTIALS);
        }

        // update basic
        ClientAppBasic appData = client;
        appData.setName(clientName);
        appData.setDisplayName(displayName);
        // NOTE: if APIm does not provide these values we will reset them to null
        appData.setGrantedTypes(new HashSet<>(grantTypes));
        // TODO check, looks like apim wants to use custom method for scopes
//        appData.setScope(new HashSet<>(Arrays.asList((scopes))));
        appData.setRedirectUris(new HashSet<>(Arrays.asList((redirectUris))));

        // NOTE: APIM client need manual Idp and scope approval from AAC

        client = clientManager.update(clientId, appData, clientSecret);

        return APIMClient.from(client);
    }

    public APIMClient updateValidity(String clientId, Integer validity)
            throws EntityNotFoundException, RuntimeException {

        // check if exists by id
        ClientAppBasic client = clientManager.findByClientId(clientId);
        if (client == null) {
            // error
            throw new EntityNotFoundException("no client with id " + clientId);
        }
        
        ClientAppBasic appData = client;
        appData.setAccessTokenValidity(validity);

        client = clientManager.update(clientId, appData);

        return APIMClient.from(client);

    }

    public APIMClient updateScope(String clientId, String scope) throws EntityNotFoundException, RuntimeException {

        // check if exists by id
        ClientAppBasic client = clientManager.findByClientId(clientId);
        if (client == null) {
            // error
            throw new EntityNotFoundException("no client with id " + clientId);
        }

        String[] scopes = scope != null ? scope.split(APIMClient.SEPARATOR) : null;

        ClientAppBasic appData = client;
        appData.setScope(new HashSet<>(Arrays.asList((scopes))));

        client = clientManager.update(clientId, appData);

        return APIMClient.from(client);

    }

    public APIMClient getClient(String clientId) throws EntityNotFoundException, RuntimeException {
        ClientAppBasic client = clientManager.findByClientId(clientId);
        if (client == null) {
            // error
            throw new EntityNotFoundException("no client with id " + clientId);
        }

        return APIMClient.from(client);

    }

    public void deleteClient(String clientId) throws EntityNotFoundException, RuntimeException {
        // TODO check if client is owned!
        ClientAppBasic client = clientManager.findByClientId(clientId);
        if (client == null) {
            // error
            throw new EntityNotFoundException("no client with id " + clientId);
        }

        clientManager.delete(clientId);
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
