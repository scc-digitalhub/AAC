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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.smartcommunitylab.aac.dto.APIKey;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.APIKeyEntity;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.repository.ApiKeyRepository;
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

	private Log log = LogFactory.getLog(getClass());

	@Autowired
	private ApiKeyRepository keyRepo;
	@Autowired
	private ClientDetailsRepository clientRepo;
	@Autowired
	private UserManager userManager;
	
	// Cache of the keys
	private static final Map<String, APIKey> keyCache = new HashMap<>();
	
	/**
	 * @param key
	 * @return true if the key exists, and either the validity is not defined (i.e., infinite) or 
	 * not yet expired
	 */
	public boolean isKeyValid(String key) {
		APIKey keyObj = findKey(key);
		if (keyObj == null) return false;
		return !keyObj.hasExpired();
	}
	
	/**
	 * @param clientId
	 * @return all API kets associated with the client
	 */
	public List<APIKey> getClientKeys(String clientId) {
		return keyRepo.findByClientId(clientId).stream().map(key -> new APIKey(key)).collect(Collectors.toList());
	}
	
	/**
	 * Find a specific API key object
	 * @param key
	 * @return
	 */
	public APIKey findKey(String key) {
		if (keyCache.containsKey(key)) {
			return keyCache.get(key);
		}
		
		log.debug("Recover API Key "+key);
		APIKeyEntity entity = keyRepo.findOne(key);
		if (entity != null) {
			APIKey result = new APIKey(entity);
			keyCache.put(key, result);
		}
		return null;
	} 
	
	/**
	 * Delete a specific key object if exists
	 * @param key
	 */
	public void deleteKey(String key) {
		keyRepo.delete(key);
		keyCache.remove(key);
		log.debug("Removed API Key "+key);
	} 
	
	/**
	 * Update key validity. Validity period is restarted
	 * @param key
	 * @param validity
	 * @return
	 * @throws #{@link EntityNotFoundException} if the key does not exists
	 */
	public APIKey updateKeyValidity(String key, Long validity) throws EntityNotFoundException {
		APIKeyEntity entity = keyRepo.findOne(key);
		if (entity != null) {
			entity.setIssuedTime(System.currentTimeMillis());
			entity.setValidity(validity);
			keyRepo.save(entity);
			APIKey result = new APIKey(entity);
			log.debug("Update API Key validity "+key);
			keyCache.put(key, result);
			return result;
		}
		throw new EntityNotFoundException(key);
	} 
	
	/**
	 * Update key additional data. 
	 * @param key
	 * @param data
	 * @return
	 * @throws #{@link EntityNotFoundException} if the key does not exists
	 */
	public APIKey updateKeyData(String key, Map<String, Object> data) throws EntityNotFoundException {
		APIKeyEntity entity = keyRepo.findOne(key);
		if (entity != null) {
			entity.setAdditionalInformation(APIKey.toDataString(data));
			keyRepo.save(entity);
			APIKey result = new APIKey(entity);
			log.debug("Update API Key data "+key);
			keyCache.put(key, result);
			return result;
		}
		throw new EntityNotFoundException(key);
	} 
	/**
	 * Create a new key for the specified client app
	 * @param clientId
	 * @param validity
	 * @param data
	 * @return
	 * @throws #{@link EntityNotFoundException} if the specified client does not exist 
	 */
	public APIKey createKey(String clientId, Long validity, Map<String, Object> data) throws EntityNotFoundException {
		ClientDetailsEntity client = clientRepo.findByClientId(clientId);
		if (client == null) throw new EntityNotFoundException("Client not found: "+clientId);
		
		APIKeyEntity entity = new APIKeyEntity();
		entity.setAdditionalInformation(APIKey.toDataString(data));
		entity.setValidity(validity);
		entity.setClientId(clientId);
		entity.setApiKey(UUID.randomUUID().toString());
		entity.setIssuedTime(System.currentTimeMillis());
		entity.setUserId(client.getDeveloperId());
		entity.setUsername(userManager.getUserInternalName(client.getDeveloperId()));
		entity.setRoles(APIKey.toRolesString(userManager.getUserRolesByClient(client.getDeveloperId(), clientId)));
		keyRepo.save(entity);
		log.debug("Saved API Key  "+entity.getApiKey());

		APIKey result = new APIKey(entity);
		keyCache.put(result.getApiKey(), result);
		return result;
	}
	
}
