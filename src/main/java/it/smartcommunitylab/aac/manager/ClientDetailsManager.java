/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package it.smartcommunitylab.aac.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.Joiner;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceScopeDTO;
import it.smartcommunitylab.aac.jaxbmodel.AuthorityMapping;
import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.model.ServiceScope;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

/**
 * Support for the management of client app registration details
 * 
 * @author raman
 *
 */
@Component
@Transactional
public class ClientDetailsManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** GRANT TYPE: CLIENT CRIDENTIALS FLOW */
//	private static final String GT_CLIENT_CREDENTIALS = "client_credentials";

//    private static final String ADMIN_GT = "implicit,refresh_token,password,client_credentials,native,authorization_code";

    private static final String[] GRANT_TYPES = {
            Config.GRANT_TYPE_AUTHORIZATION_CODE,
            Config.GRANT_TYPE_IMPLICIT,
            Config.GRANT_TYPE_CLIENT_CREDENTIALS,
            Config.GRANT_TYPE_NATIVE,
            Config.GRANT_TYPE_PASSWORD,
            Config.GRANT_TYPE_REFRESH_TOKEN,
            Config.GRANT_TYPE_DEVICE_CODE
    };

    @Autowired
    private ClientDetailsRepository clientDetailsRepository;

    @Autowired
    private AttributesAdapter attributesAdapter;

    @Autowired
    private ServiceManager serviceManager;

//	@Value("${adminClient.id:}")
//	private String adminClientId;
//	@Value("${adminClient.secret:}")
//	private String adminClientSecret;
//	@Value("${adminClient.scopes:}")
//	private String[] adminClientScopes;
//	@Value("${adminClient.redirects:}")
//	private String[] adminClientRedirects;

    @PostConstruct
    public void init() {
        clientDetailsRepository.findAll().forEach(c -> {
            Set<String> scope = c.getScope();
            if (scope != null && !scope.isEmpty()) {
                c.setResourceIds(StringUtils
                        .collectionToCommaDelimitedString(serviceManager.findServiceIdsByScopes(c.getScope())));
                clientDetailsRepository.save(c);
            }
        });
    }

    /**
     * Create a new empty Client from {@link ClientAppBasic} descriptor
     * 
     * @param appData
     * @param userId
     * @return {@link ClientAppBasic} descriptor of the created Client
     * @throws Exception
     */
    public ClientAppBasic create(ClientAppBasic appData, Long userId) throws Exception {
        ClientDetailsEntity entity = new ClientDetailsEntity();
        ClientAppInfo info = new ClientAppInfo();

        if (!StringUtils.hasText(appData.getName())) {
            throw new IllegalArgumentException("An app name cannot be empty");
        }

        ClientDetailsEntity old = clientDetailsRepository.findByName(appData.getName());
        if (old != null) {
            throw new IllegalArgumentException("An app with the same name already exists");
        }

        info.setName(appData.getName());
        entity.setName(appData.getName());
        entity.setAdditionalInformation(info.toJson());
        entity.setClientId(generateClientId());
        entity.setAuthorities(Config.AUTHORITY.ROLE_CLIENT.toString());
        entity.setAuthorizedGrantTypes(StringUtils.arrayToCommaDelimitedString(defaultGrantTypes()));
        entity.setDeveloperId(userId);
        entity.setClientSecret(generateClientSecret());
        entity.setScope(appData.getScope());
        entity.setClientSecretMobile(generateClientSecret());
        entity.setParameters(appData.getParameters());

        entity = clientDetailsRepository.save(entity);
        return convertToClientApp(entity);

    }

    /**
     * Create a new complete Client from {@link ClientAppBasic} descriptor
     * 
     * @param appData
     * @param userId
     * @return {@link ClientAppBasic} descriptor of the created Client
     * @throws Exception
     */
    public ClientAppBasic create(ClientAppBasic appData, Long userId,
            String clientId, String clientSecret, String clientSecretMobile) throws IllegalArgumentException {
        return this.create(clientId, userId, appData, clientSecret, clientSecretMobile,
                Config.AUTHORITY.ROLE_CLIENT.toString());
    }

    /**
     * Create trusted client app
     * 
     * @return
     * @throws Exception
     */
    public ClientAppBasic createTrusted(ClientAppBasic appData, Long userId,
            String clientId, String clientSecret, String clientSecretMobile) throws IllegalArgumentException {
        return this.create(clientId, userId, appData, clientSecret, clientSecretMobile,
                Config.AUTHORITY.ROLE_CLIENT_TRUSTED.toString());
    }

    /**
     * Create a new complete Client from {@link ClientAppBasic} descriptor
     * 
     * @param appData
     * @param userId
     * @return {@link ClientAppBasic} descriptor of the created Client
     * @throws Exception
     */
    protected ClientAppBasic create(String clientId, Long userId,
            ClientAppBasic appData,
            String clientSecret, String clientSecretMobile,
            String clientAuthorities) throws IllegalArgumentException {
        ClientDetailsEntity client = new ClientDetailsEntity();

        if (!StringUtils.hasText(clientId)) {
            throw new IllegalArgumentException("Client id cannot be empty");
        }

        if (!StringUtils.hasText(appData.getName())) {
            throw new IllegalArgumentException("An app name cannot be empty");
        }

        if (!Arrays.asList(GRANT_TYPES).containsAll(appData.getGrantedTypes())) {
            throw new IllegalArgumentException("Invalid grant types");
        }

        if (!StringUtils.hasText(clientSecret)) {
            clientSecret = generateClientSecret();
        }

        if (!StringUtils.hasText(clientSecretMobile)) {
            clientSecretMobile = generateClientSecret();
        }

        ClientDetailsEntity old = clientDetailsRepository.findByClientId(clientId);
        if (old != null) {
            throw new IllegalArgumentException("An app with the same id already exists");
        }

        // set base fields
        client.setName(appData.getName());
        client.setClientId(clientId);
        client.setAuthorities(clientAuthorities);
        client.setDeveloperId(userId);
        client.setClientSecret(clientSecret);
        client.setClientSecretMobile(clientSecretMobile);

        // convert additional fields
        client = convertFromClientApp(client, appData);

        if (client == null) {
            throw new IllegalArgumentException("");
        }

        // always auto-enable internal for trusted clients
        if (Config.AUTHORITY.ROLE_CLIENT_TRUSTED.toString().equals(clientAuthorities)) {
            ClientAppInfo info = ClientAppInfo.convert(client.getAdditionalInformation());
            info.getIdentityProviders().put(Config.IDP_INTERNAL, ClientAppInfo.APPROVED);
            // also update scope approvals
            info.setScopeApprovals(Collections.<String, Boolean>emptyMap());

            client.setAdditionalInformation(info.toJson());
        }

        client = clientDetailsRepository.save(client);
        return convertToClientApp(client);
    }

    /**
     * Delete the specified client
     * 
     * @param clientId
     * @return {@link ClientAppBasic} descriptor of the deleted client
     */
    public ClientAppBasic delete(String clientId) {
        ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
        if (client == null) {
            throw new EntityNotFoundException("client app not found");
        }
        clientDetailsRepository.delete(client);
        return convertToClientApp(client);
    }

    /**
     * Update client info
     * 
     * @param clientId
     * @param data
     * @return
     */
    public ClientAppBasic update(String clientId, ClientAppBasic data)
            throws EntityNotFoundException, IllegalArgumentException {
        ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
        String error = null;
        if ((error = validate(client, data)) != null) {
            throw new IllegalArgumentException(error);
        }

//        client = convertFromClientApp(client, data);
//        if (client == null) {
//            logger.error("Problem converting the client");
//            throw new IllegalArgumentException("internal error");
//        }
//
//        clientDetailsRepository.save(client);
//        return convertToClientApp(client);

        return update(clientId, data, null, null, null);
    }

    /**
     * Update an existing standard Client from {@link ClientAppBasic} descriptor
     * 
     * @param appData
     * @param userId
     * @return {@link ClientAppBasic} descriptor of the created Client
     * @throws Exception
     */
    public ClientAppBasic update(String clientId,
            ClientAppBasic appData,
            String clientSecret, String clientSecretMobile) throws EntityNotFoundException, IllegalArgumentException {
        return update(clientId, appData, clientSecret, clientSecretMobile, Config.AUTHORITY.ROLE_CLIENT.toString());
    }

    /**
     * Update an existing trusted Client from {@link ClientAppBasic} descriptor
     * 
     * @param appData
     * @param userId
     * @return {@link ClientAppBasic} descriptor of the created Client
     * @throws Exception
     */
    public ClientAppBasic updateTrusted(String clientId,
            ClientAppBasic appData,
            String clientSecret, String clientSecretMobile) throws EntityNotFoundException, IllegalArgumentException {
        return update(clientId, appData, clientSecret, clientSecretMobile,
                Config.AUTHORITY.ROLE_CLIENT_TRUSTED.toString());
    }

    /**
     * Update an existing Client from {@link ClientAppBasic} descriptor
     * 
     * @param appData
     * @param userId
     * @return {@link ClientAppBasic} descriptor of the created Client
     * @throws Exception
     */
    protected ClientAppBasic update(String clientId,
            ClientAppBasic appData,
            String clientSecret, String clientSecretMobile,
            String clientAuthorities) throws EntityNotFoundException, IllegalArgumentException {

        if (!StringUtils.hasText(clientId)) {
            throw new IllegalArgumentException("Client id cannot be empty");
        }

        ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);

        if (client == null) {
            throw new EntityNotFoundException("client app not found");
        }

        if (!StringUtils.hasText(appData.getName())) {
            throw new IllegalArgumentException("An app name cannot be empty");
        }

        if (appData.getGrantedTypes() != null) {
            if (!Arrays.asList(GRANT_TYPES).containsAll(appData.getGrantedTypes())) {
                throw new IllegalArgumentException("Invalid grant types");
            }
        }

        // set base fields
        client.setName(appData.getName());

        if (StringUtils.hasText(clientAuthorities)) {
            client.setAuthorities(clientAuthorities);
        }
        if (StringUtils.hasText(clientSecret)) {
            client.setClientSecret(clientSecret);
        }
        if (StringUtils.hasText(clientSecretMobile)) {
            client.setClientSecretMobile(clientSecretMobile);
        }
        // convert additional fields
        client = convertFromClientApp(client, appData);

        if (client == null) {
            throw new IllegalArgumentException("");
        }

        // always auto-enable internal for trusted clients
        if (Config.AUTHORITY.ROLE_CLIENT_TRUSTED.toString().equals(clientAuthorities)) {
            ClientAppInfo info = ClientAppInfo.convert(client.getAdditionalInformation());
            info.getIdentityProviders().put(Config.IDP_INTERNAL, ClientAppInfo.APPROVED);
            // also update scope approvals
            info.setScopeApprovals(Collections.<String, Boolean>emptyMap());

            client.setAdditionalInformation(info.toJson());
        }

        client = clientDetailsRepository.save(client);
        return convertToClientApp(client);
    }

//	/**
//	 * Create default client app for Admin user. The configuration properties are defined through the environment variables.
//	 * @param adminId
//	 * @return
//	 * @throws Exception
//	 */
//	public ClientAppBasic createAdminClient(Long adminId) throws Exception {
//		if (StringUtils.isEmpty(adminClientId)) return null;
//		
//		ClientDetailsEntity client = clientDetailsRepository.findByClientId(adminClientId);
//		if (client == null) {
//			client = new ClientDetailsEntity();
//			
//			client.setName(adminClientId);
//			client.setClientId(adminClientId);
//			client.setAuthorities(Config.AUTHORITY.ROLE_CLIENT_TRUSTED.toString());
//			client.setAuthorizedGrantTypes(ADMIN_GT);
//			client.setDeveloperId(adminId);
//			client.setClientSecret(adminClientSecret);
//			client.setClientSecretMobile(generateClientSecret());
//			client.setRedirectUri(StringUtils.arrayToCommaDelimitedString(adminClientRedirects));
//			client.setMobileAppSchema(adminClientId);
//			
//			ClientAppInfo info = new ClientAppInfo();
//			info.setDisplayName(adminClientId);
//			info.setName(adminClientId);
//			info.setIdentityProviders(Collections.singletonMap(Config.IDP_INTERNAL, ClientAppInfo.APPROVED));
//
//			info.setScopeApprovals(Collections.<String,Boolean>emptyMap());
//			client.setAdditionalInformation(info.toJson());
//			final Set<String> uriSet = new HashSet<>();
//			final Set<String> idSet = new HashSet<>();
//			for (String scope : adminClientScopes) {
//				ServiceScope scopeObj = serviceManager.getServiceScope(scope);
//				if (scopeObj != null) {
//					uriSet.add(scopeObj.getScope());
//					idSet.add(scopeObj.getService().getServiceId());
//				}
//			}
//			client.setResourceIds(StringUtils.collectionToCommaDelimitedString(idSet));
//			client.setScope(StringUtils.collectionToCommaDelimitedString(uriSet));
//
//			clientDetailsRepository.save(client);
//		}
//		return convertToClientApp(client);
//	}

    /**
     * Validate correctness of the data specified for the app
     * 
     * @param client
     * @param data
     */
    public String validate(ClientDetailsEntity client, ClientAppBasic data) {
        if (client == null) {
            return "app not found";
        }
        // name should not be empty
        if (data.getName() == null || data.getName().trim().isEmpty()) {
            return "name cannot be empty";
        }
        // for server-side or native access redirect URLs are required
        if ((data.hasServerSideAccess() || data.isNativeAppsAccess())
                && (data.getRedirectUris() == null || data.getRedirectUris().trim().isEmpty())) {
            return "redirect URL is required for Server-side or native access";
        }
//		if (data.isNativeAppsAccess() && (data.getNativeAppSignatures() == null || data.getNativeAppSignatures().isEmpty())) {
//			return "app signature is required for native access";
//		}
        return null;
    }

    /**
     * Reset clientSecretMobile
     * 
     * @param clientId
     * @return updated {@link ClientDetailsEntity} instance
     */
    public ClientAppBasic resetClientSecretMobile(String clientId) {
        return convertToClientApp(resetClientData(clientId, true));
    }

    /**
     * Reset client secret
     * 
     * @param clientId
     * @return updated {@link ClientDetailsEntity} instance
     */
    public ClientAppBasic resetClientSecret(String clientId) {
        return convertToClientApp(resetClientData(clientId, false));
    }

    public ClientDetailsEntity resetClientData(String clientId, boolean resetClientSecretMobile) {
        ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
        if (client == null) {
            throw new IllegalArgumentException("client app not found");
        }
        if (resetClientSecretMobile) {
            client.setClientSecretMobile(generateClientSecret());
        } else {
            client.setClientSecret(generateClientSecret());
        }
        clientDetailsRepository.save(client);
        return client;
    }

    /**
     * Return the set of identity providers allowed for the client
     * 
     * @param clientId
     * @return set of identity providers IDs (authorities)
     */
    public Set<String> getIdentityProviders(String clientId) {
        ClientDetailsEntity entity = clientDetailsRepository.findByClientId(clientId);
        if (entity == null) {
            throw new IllegalArgumentException("client not found");
        }
        ClientAppInfo info = ClientAppInfo.convert(entity.getAdditionalInformation());
        Set<String> res = new HashSet<String>();
        if (info.getIdentityProviders() != null) {
            for (String s : info.getIdentityProviders().keySet()) {
                if (ClientAppInfo.APPROVED == info.getIdentityProviders().get(s)) {
                    res.add(s);
                }
            }
        }
        return res;
    }

    public ClientAppBasic approveClientIdp(String clientId) throws Exception {
        ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
        if (client == null) {
            throw new IllegalArgumentException("client app not found");
        }

        ClientAppInfo info = ClientAppInfo.convert(client.getAdditionalInformation());
        if (!info.getIdentityProviders().isEmpty()) {
            for (String key : info.getIdentityProviders().keySet()) {
                info.getIdentityProviders().put(key, ClientAppInfo.APPROVED);
            }
            client.setAdditionalInformation(info.toJson());
            client = clientDetailsRepository.save(client);
        }
        return convertToClientApp(client);
    }

    public ClientAppBasic approveClientScopes(String clientId) throws Exception {
        ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
        if (client == null) {
            throw new IllegalArgumentException("client app not found");
        }

        ClientAppInfo info = ClientAppInfo.convert(client.getAdditionalInformation());
        if (info.getScopeApprovals() != null && !info.getScopeApprovals().isEmpty()) {
            Set<String> newScopeSet = new HashSet<String>();
            if (client.getScope() != null) {
                newScopeSet.addAll(client.getScope());
            }
            for (String rId : info.getScopeApprovals().keySet()) {
                ServiceScopeDTO resource = serviceManager.getServiceScopeDTO(rId);
                newScopeSet.add(resource.getScope());
            }

            client.setScope(StringUtils.collectionToCommaDelimitedString(newScopeSet));
            client.setResourceIds(
                    StringUtils.collectionToCommaDelimitedString(serviceManager.findServiceIdsByScopes(newScopeSet)));
            info.setScopeApprovals(Collections.<String, Boolean>emptyMap());
            client.setAdditionalInformation(info.toJson());
            client = clientDetailsRepository.save(client);
        }

        return convertToClientApp(client);
    }

    /**
     * @param userId
     * @return {@link List} of {@link ClientAppBasic} objects representing client
     *         apps
     */
    public List<ClientAppBasic> getByDeveloperId(Long userId) {
        return convertToClientApps(clientDetailsRepository.findByDeveloperId(userId));
    }

    /**
     * @param clientId
     * @return {@link ClientAppBasic} object representing client app
     */
    public ClientAppBasic getByClientId(String clientId) throws EntityNotFoundException {
        ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
        if (client == null) {
            throw new EntityNotFoundException();
        }
        return convertToClientApp(client);
    }

    /**
     * @param clientId
     * @return {@link ClientAppBasic} object representing client app
     */
    public ClientAppBasic findByClientId(String clientId) {
        ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
        if (client == null) {
            return null;
        }
        return convertToClientApp(client);
    }

    /**
     * Create or update a Client from {@link ClientAppBasic} descriptor
     * 
     * @param appData
     * @param userId
     * @return {@link ClientAppBasic} descriptor of the created Client
     * @throws Exception
     */
    public ClientAppBasic createOrUpdate(ClientAppBasic appData, Long userId) throws Exception {
        ClientDetailsEntity entity = new ClientDetailsEntity();
        ClientAppInfo info = new ClientAppInfo();
        if (!StringUtils.hasText(appData.getName())) {
            throw new IllegalArgumentException("An app name cannot be empty");
        }
        info.setName(appData.getName());

        for (ClientDetailsEntity cde : clientDetailsRepository.findAll()) {
            ClientAppInfo.convert(cde.getAdditionalInformation());
        }

        ClientDetailsEntity old = clientDetailsRepository.findByName(appData.getName());
        if (old != null) {
            entity = old;
        }

        entity.setAdditionalInformation(info.toJson());
        entity.setName(appData.getName());
        entity.setClientId(generateClientId());
        entity.setAuthorities(Config.AUTHORITY.ROLE_CLIENT.toString());
        entity.setAuthorizedGrantTypes(StringUtils.arrayToCommaDelimitedString(defaultGrantTypes()));
        entity.setDeveloperId(userId);
        entity.setClientSecret(generateClientSecret());
        entity.setScope(appData.getScope());
        entity.setClientSecretMobile(generateClientSecret());
        entity.setParameters(appData.getParameters());

        entity = clientDetailsRepository.save(entity);
        return convertToClientApp(entity);

    }

    /**
     * Convert DB object to the simplified client representation
     * 
     * @param e
     * @return
     */
    public ClientAppBasic convertToClientApp(ClientDetailsEntity e) {
        ClientAppBasic res = new ClientAppBasic();

        res.setClientId(e.getClientId());
        res.setClientSecret(e.getClientSecret());
        res.setClientSecretMobile(e.getClientSecretMobile());
        res.setGrantedTypes(e.getAuthorizedGrantTypes());
        res.setUserName(e.getDeveloperId().toString());

        // approval status
        res.setIdentityProviderApproval(new HashMap<String, Boolean>());

        // request status
        res.setIdentityProviders(new HashMap<String, Boolean>());
        for (String key : attributesAdapter.getAuthorityUrls().keySet()) {
            res.getIdentityProviders().put(key, false);
        }

        res.setName(e.getName());
        res.setDisplayName(res.getName());
        res.setScope(Joiner.on(",").join(e.getScope()));
        res.setParameters(e.getParameters());
        res.setMobileAppSchema(e.getMobileAppSchema());

        ClientAppInfo info = ClientAppInfo.convert(e.getAdditionalInformation());
        if (info != null) {
            res.setDisplayName(info.getDisplayName());
            if (info.getIdentityProviders() != null) {
                for (String key : info.getIdentityProviders().keySet()) {
                    switch (info.getIdentityProviders().get(key)) {
                    case ClientAppInfo.APPROVED:
                        res.getIdentityProviderApproval().put(key, true);
                        res.getIdentityProviders().put(key, true);
                        break;
                    case ClientAppInfo.REJECTED:
                        res.getIdentityProviderApproval().put(key, false);
                        res.getIdentityProviders().put(key, true);
                        break;
                    case ClientAppInfo.REQUESTED:
                        res.getIdentityProviders().put(key, true);
                        break;
                    default:
                        break;
                    }
                }
            }

            res.setProviderConfigurations(info.getProviderConfigurations());
            res.setClaimMapping(info.getClaimMapping());
            res.setUniqueSpaces(info.getUniqueSpaces());
            res.setOnAfterApprovalWebhook(info.getOnAfterApprovalWebhook());
        }

        res.setRedirectUris(StringUtils.collectionToCommaDelimitedString(e.getRegisteredRedirectUri()));

        if (e.getAccessTokenValiditySeconds() != null) {
            res.setAccessTokenValidity(e.getAccessTokenValiditySeconds());
        } else {
            res.setAccessTokenValidity(0);
        }
        if (e.getRefreshTokenValiditySeconds() != null) {
            res.setRefreshTokenValidity(e.getRefreshTokenValiditySeconds());
        } else {
            res.setRefreshTokenValidity(0);
        }

        return res;
    }

    /**
     * Convert DB objects to the simplified client representation
     * 
     * @param entities
     * @return
     */
    private List<ClientAppBasic> convertToClientApps(List<ClientDetailsEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
//        List<ClientAppBasic> res = new ArrayList<ClientAppBasic>();
//        for (ClientDetailsEntity e : entities) {
//            res.add(convertToClientApp(e));
//        }
//        return res;
        return entities.stream().map(e -> convertToClientApp(e)).collect(Collectors.toList());

    }

    /**
     * Fill in the DB object with the properties of {@link ClientAppBasic} instance.
     * In case of problem, return null.
     * 
     * @param client
     * @param data
     * @return
     * @throws Exception
     */
    private ClientDetailsEntity convertFromClientApp(ClientDetailsEntity client, ClientAppBasic data) {
        try {
            ClientAppInfo info = null;

            if (client.getAdditionalInformation() == null) {
                info = new ClientAppInfo();
            } else {
                info = ClientAppInfo.convert(client.getAdditionalInformation());
            }

            info.setName(data.getName());

            if (StringUtils.hasText(data.getDisplayName())) {
                info.setDisplayName(data.getDisplayName());
            } else {
                info.setDisplayName(info.getName());
            }

            if (StringUtils.hasText(data.getMobileAppSchema())) {
                client.setMobileAppSchema(data.getMobileAppSchema());
            } else {
                client.setMobileAppSchema(generateSchema(client.getClientId()));
            }

            if (info.getIdentityProviders() == null) {
                info.setIdentityProviders(new HashMap<String, Integer>());
            }

            if (data.getIdentityProviders() != null) {
                for (String key : attributesAdapter.getAuthorityUrls().keySet()) {
                    if (data.getIdentityProviders().get(key)) {
                        Integer value = info.getIdentityProviders().get(key);
                        AuthorityMapping a = attributesAdapter.getAuthority(key);
                        if (value == null || value == ClientAppInfo.UNKNOWN) {
                            info.getIdentityProviders().put(key,
                                    a.isPublic() ? ClientAppInfo.APPROVED : ClientAppInfo.REQUESTED);
                        }
                    } else {
                        info.getIdentityProviders().remove(key);
                    }

                    if (data.getProviderConfigurations() != null && data.getProviderConfigurations().containsKey(key)) {
                        if (info.getProviderConfigurations() == null) {
                            info.setProviderConfigurations(new HashMap<>());
                        }

                        info.getProviderConfigurations().put(key, data.getProviderConfigurations().get(key));
                    }
                }
            }

            // update additional fields only if non-null
            // to reset fields pass an empty string
            if (data.getClaimMapping() != null) {
                info.setClaimMapping(data.getClaimMapping());
            }

            if (data.getUniqueSpaces() != null) {
                info.setUniqueSpaces(data.getUniqueSpaces());
            }

            if (data.getUniqueSpaces() != null) {
                info.setOnAfterApprovalWebhook(data.getOnAfterApprovalWebhook());
            }

            client.setAdditionalInformation(info.toJson());

            if (data.getRedirectUris() != null) {
                client.setRedirectUri(Utils.normalizeValues(data.getRedirectUris()));
            }

            // pass an empty set to reset
            if (data.getGrantedTypes() != null) {
                Set<String> types = data.getGrantedTypes();
                client.setAuthorizedGrantTypes(StringUtils.collectionToCommaDelimitedString(types));
            }

            if (data.getScope() != null) {
                client.setScope(data.getScope());
            }

            if (!StringUtils.isEmpty(data.getScope())) {
                Set<String> serviceIds = serviceManager.findServiceIdsByScopes(client.getScope());
                client.setResourceIds(StringUtils.collectionToCommaDelimitedString(serviceIds));
            }

            if (data.getParameters() != null) {
                client.setParameters(data.getParameters());
            }

            if (data.getAccessTokenValidity() != null) {
                if (data.getAccessTokenValidity().intValue() > 0) {
                    client.setAccessTokenValidity(data.getAccessTokenValidity());
                } else {
                    client.setAccessTokenValidity(null);
                }
            }

            if (data.getRefreshTokenValidity() != null) {
                if (data.getRefreshTokenValidity().intValue() > 0) {
                    client.setRefreshTokenValidity(data.getRefreshTokenValidity());
                } else {
                    client.setRefreshTokenValidity(null);
                }
            }
        } catch (Exception e) {
            logger.error("failed to convert an object: " + e.getMessage(), e);
            return null;
        }

        return client;
    }

//  /**
//   * Client authorities to be associated with client app by default
//   * @return
//   */
//  public String defaultAuthorities() {
//      return Config.AUTHORITY.ROLE_CLIENT.toString();
//  }

    /**
     * Client types to be associated with client app by default
     * 
     * @return
     */
    private String[] defaultGrantTypes() {
        return new String[] { Config.GRANT_TYPE_CLIENT_CREDENTIALS };
    }

    /**
     * Generate new value to be used as clientId (String)
     * 
     * @return
     */
    private synchronized String generateClientId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate new value to be used as client secret (String)
     * 
     * @return
     */
    private synchronized String generateClientSecret() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate schema identifier
     * 
     * @return
     */
    private String generateSchema(String clientId) {
        return clientId;
    }

}
