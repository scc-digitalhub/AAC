/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
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
 ******************************************************************************/

package it.smartcommunitylab.aac.apikey;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import it.smartcommunitylab.aac.apikey.model.APIKey;
import it.smartcommunitylab.aac.apikey.model.APIKeyEntity;
import it.smartcommunitylab.aac.apikey.repository.ApiKeyRepository;
import it.smartcommunitylab.aac.manager.ClaimManager;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

/**
 * Manager for APIKey entities
 * 
 * @author raman
 *
 */
@Component
@Transactional
public class APIKeyManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // TODO replace with keyService
    @Autowired
    private ApiKeyRepository keyRepo;

    // TODO replace with clientService
    @Autowired
    private ClientDetailsRepository clientRepo;

    @Autowired
    private UserManager userManager;

    @Autowired
    private RoleManager roleManager;

    @Autowired
    private ClaimManager claimManager;

    @Value("${jwt.issuer}")
    private String issuer;

    // TODO move to keyService
//    // Cache of the keys
//    private static final Map<String, APIKey> keyCache = new HashMap<>();

    LoadingCache<String, APIKey> keysCache = CacheBuilder
            .newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES) // expires 15m after fetch
            .build(new CacheLoader<String, APIKey>() {
                @Override
                public APIKey load(String key) throws Exception {
                    APIKeyEntity entity = keyRepo.findOne(key);
                    if (entity == null) {
                        throw new EntityNotFoundException();
                    }

                    return APIKey.fromApiKey(entity);
                }

            });

    /**
     * Find a specific API key object
     * 
     * @param key
     * @return
     */
    public APIKey findKey(String key) {
        logger.debug("search API Key " + key);
        try {
            return keysCache.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get a specific API key object
     * 
     * @param key
     * @return
     */
    public APIKey getKey(String key) {
        logger.debug("get API Key " + key);
        try {
            // clone to avoid updating object in cache
            APIKey apikey = new APIKey(keysCache.get(key));
            apikey.setIssuer(issuer);
            String clientId = apikey.getClientId();
            long userId = Long.parseLong(apikey.getSubject());
            Set<String> scope = new HashSet<>(Arrays.asList(apikey.getScope()));

            if (!scope.isEmpty()) {
                ClientDetailsEntity client = clientRepo.findByClientId(clientId);
                if (client == null) {
                    logger.error("client not found for apikey");
                    throw new EntityNotFoundException("client not found: " + clientId);
                }

                User user = userManager.getOne(userId);
                if (user == null) {
                    logger.error("user not found for apikey");
                    throw new EntityNotFoundException("user not found: " + userId);
                }

                // refresh authorities since authentication could be stale (stored in db)
                List<GrantedAuthority> userAuthorities = roleManager.buildAuthorities(user);
                // populate user fields
                // TODO cache these
                Map<String, Object> userClaims = claimManager.getUserClaims(user.getId().toString(), userAuthorities,
                        client, scope, null, null);
                apikey.setUserClaims(userClaims);
            }

            return apikey;

        } catch (Exception e) {
            return null;
        }
    }

    // TODO move to keyService
    /**
     * @param key
     * @return true if the key exists, and either the validity is not defined (i.e.,
     *         infinite) or not yet expired
     */
    public boolean isKeyValid(String key) {
        try {
            APIKey apikey = keysCache.get(key);
            return (apikey.getExpirationTime() == 0 ? true
                    : apikey.getExpirationTime() > (System.currentTimeMillis() / 1000));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param clientId
     * @return all API keys associated with the client
     */
    public List<APIKey> getClientKeys(String clientId) {
        return keyRepo.findByClientId(clientId)
                .stream()
                .map(e -> APIKey.fromApiKey(e)).collect(Collectors.toList());
    }

    /**
     * @param userId
     * @return all API keys created by the user
     */
    public List<APIKey> getUserKeys(String userId) {
        return keyRepo.findByUserId(Long.parseLong(userId))
                .stream()
                .map(e -> APIKey.fromApiKey(e)).collect(Collectors.toList());
    }

    /**
     * Delete a specific key object if exists
     * 
     * @param key
     */
    public void deleteKey(String key) {
        keysCache.invalidate(key);
        keyRepo.delete(key);
        logger.debug("removed API Key " + key);
    }

    
    
    //TODO: move to keyService generic method, implement here 2 methods createClientKey and createUserKey
    /**
     * Create a new key for the specified client app
     * 
     * @param clientId
     * @param validity
     * @param data
     * @return
     * @throws #{@link EntityNotFoundException} if the specified client does not
     *                 exist
     */
    public APIKey createKey(String clientId, String userId, int validity, Map<String, Object> data, Set<String> scopes)
            throws EntityNotFoundException {

        ClientDetailsEntity client = clientRepo.findByClientId(clientId);
        if (client == null) {
            throw new EntityNotFoundException("Client not found: " + clientId);
        }

        if (StringUtils.isEmpty(userId)) {
            // use developer as owner
            userId = Long.toString(client.getDeveloperId());
        }

        User user = userManager.getOne(Long.parseLong(userId));
        if (user == null) {
            logger.error("user not found for apikey");
            throw new EntityNotFoundException("user not found: " + userId);
        }

        String key = generateKey();

        APIKeyEntity entity = new APIKeyEntity();
        entity.setApiKey(key);
        entity.setClientId(clientId);
        entity.setAdditionalInformation(APIKey.toDataString(data));
        entity.setUserId(user.getId());

        if (scopes != null && !scopes.isEmpty()) {
            Set<String> targetScopes = new HashSet<>(scopes);
            targetScopes.retainAll(client.getScope());
            entity.setScope(StringUtils.collectionToCommaDelimitedString(targetScopes));
        } else {
            entity.setScope("");
        }

        entity.setIssuedTime(System.currentTimeMillis());

        if (validity == 0) {
            // no expiration
            entity.setValidity(null);
        } else {
            // in seconds
            entity.setValidity(Long.valueOf(validity));
        }

        // legacy fields
        // TODO remove
        entity.setUsername(null);
        entity.setRoles(null);

        entity = keyRepo.saveAndFlush(entity);
        logger.debug("Saved API Key  " + entity.getApiKey());

        APIKey apikey = APIKey.fromApiKey(entity);
        keysCache.put(key, apikey);

        return apikey;
    }

    /**
     * Update key. Validity period is restarted
     * 
     * @param key
     * @param validity
     * @return
     * @throws #{@link EntityNotFoundException} if the key does not exists
     */
    public APIKey updateKey(String key, Integer validity, Set<String> scopes, Map<String, Object> data)
            throws EntityNotFoundException {
        APIKeyEntity entity = keyRepo.findOne(key);
        if (entity == null) {
            throw new EntityNotFoundException(key);
        }

        logger.debug("update API Key validity " + key);

        // reset issued time to now
        entity.setIssuedTime(System.currentTimeMillis());

        if (validity != null) {
            if (validity == 0) {
                entity.setValidity(null);
            } else {
                entity.setValidity(Long.valueOf(validity));
            }
        }

        if (data != null) {
            entity.setAdditionalInformation(APIKey.toDataString(data));
        }

        if (scopes != null) {
            // filter and allow only those enabled on client
            ClientDetailsEntity client = clientRepo.findByClientId(entity.getClientId());
            Set<String> targetScopes = new HashSet<>(scopes);
            targetScopes.retainAll(client.getScope());
            entity.setScope(StringUtils.collectionToCommaDelimitedString(targetScopes));
        } else {
            // reset scopes
            entity.setScope("");
        }

        entity = keyRepo.saveAndFlush(entity);

        APIKey apikey = APIKey.fromApiKey(entity);
        keysCache.put(key, apikey);

        return apikey;

    }

    /**
     * Update key validity. Validity period is restarted
     * 
     * @param key
     * @param validity
     * @return
     * @throws #{@link EntityNotFoundException} if the key does not exists
     */
    public APIKey updateKeyValidity(String key, int validity) throws EntityNotFoundException {
        APIKeyEntity entity = keyRepo.findOne(key);
        if (entity == null) {
            throw new EntityNotFoundException(key);
        }

        logger.debug("update API Key " + key);

        // reset issued time to now
        entity.setIssuedTime(System.currentTimeMillis());
        if (validity == 0) {
            entity.setValidity(null);
        } else {
            entity.setValidity(Long.valueOf(validity));
        }
        entity = keyRepo.saveAndFlush(entity);

        APIKey apikey = APIKey.fromApiKey(entity);
        keysCache.put(key, apikey);

        return apikey;

    }

    /**
     * Update key additional data.
     * 
     * @param key
     * @param data
     * @return
     * @throws #{@link EntityNotFoundException} if the key does not exists
     */
    public APIKey updateKeyData(String key, Map<String, Object> data) throws EntityNotFoundException {
        APIKeyEntity entity = keyRepo.findOne(key);
        if (entity == null) {
            throw new EntityNotFoundException(key);
        }

        // update only additional info, no validity extension
        entity.setAdditionalInformation(APIKey.toDataString(data));

        entity = keyRepo.saveAndFlush(entity);

        APIKey apikey = APIKey.fromApiKey(entity);
        keysCache.put(key, apikey);

        return apikey;

    }

    /**
     * Update key scopes.
     * 
     * @param key
     * @param data
     * @return
     * @throws #{@link EntityNotFoundException} if the key does not exists
     */
    public APIKey updateKeyScopes(String key, Set<String> scopes) throws EntityNotFoundException {
        APIKeyEntity entity = keyRepo.findOne(key);
        if (entity == null) {
            throw new EntityNotFoundException(key);
        }

        if (scopes != null) {
            // filter and allow only those enabled on client
            // TODO additionally filter to only those approved by user
            ClientDetailsEntity client = clientRepo.findByClientId(entity.getClientId());
            Set<String> targetScopes = new HashSet<>(scopes);
            targetScopes.retainAll(client.getScope());
            entity.setScope(StringUtils.collectionToCommaDelimitedString(targetScopes));
        } else {
            // reset scopes
            entity.setScope("");
        }

        entity = keyRepo.saveAndFlush(entity);

        APIKey apikey = APIKey.fromApiKey(entity);
        keysCache.put(key, apikey);

        return apikey;

    }

    /*
     * Generate a secure key
     */
    private String generateKey() {
        // use a random UUID as key
        return UUID.randomUUID().toString();

    }

}
