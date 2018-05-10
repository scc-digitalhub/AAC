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

package it.smartcommunitylab.aac.apimanager;

import java.util.List;

import it.smartcommunitylab.aac.apimanager.model.AACAPI;
import it.smartcommunitylab.aac.apimanager.model.DataList;
import it.smartcommunitylab.aac.apimanager.model.Subscription;

/**
 * Interface towards integration with the API Manager
 * 
 * @author raman
 *
 */
public interface APIManager {

	/**
	 * Publish API on the API Manager, given metadata, token and JSON model of the Swagger API definition.
	 * @param jsonModel Swagger API definition
	 * @param name name of the service to publish
	 * @param description String description of the API 
	 * @param context context path
	 * @param token of the API Provider to publish the API with
	 * @throws Exception in case of operation failure
	 */
	void publishAPI(String jsonModel, String name, String description, String context, String token) throws Exception;
	
	/**
	 * Get APIs that match a query string with pagination.
	 * @param offset page start
	 * @param limit page size
	 * @param query search query
	 * @param token access token
	 * @return
	 * @throws Exception
	 */
	DataList<AACAPI> getAPIs(Integer offset, Integer limit, String query, String token) throws Exception;
	
	/**
	 * Get API by Id
	 * @param apiId
	 * @param token
	 * @return
	 * @throws Exception
	 */
	AACAPI getAPI(String apiId, String token) throws Exception;
	
	/**
	 * find APIs with the specified name
	 * @param name
	 * @param token access token
	 * @return
	 * @throws Exception
	 */
	DataList<AACAPI> findAPI(String name, String token) throws Exception;
	
	/**
	 * Get API image
	 * @param apiId
	 * @param token access token
	 * @return
	 * @throws Exception
	 */
	byte[] getAPIThumbnail(String apiId, String token) throws Exception;
	
	/**
	 * Get API subscriptions with pagination
	 * @param apiId id of API
	 * @param tenant API tenant
	 * @param offset page offset
	 * @param limit page size
	 * @param token access token
	 * @return
	 * @throws Exception
	 */
	DataList<Subscription> getSubscriptions(String apiId, String tenant, Integer offset, Integer limit, String token) throws Exception;
	
	/**
	 * Get subscriptions of a specific Application 
	 * @param applicationName
	 * @param token
	 * @return
	 * @throws Exception
	 */
	List<Subscription> getSubscriptions(String applicationName, String token) throws Exception;

	/**
	 * Subscribe an API
	 * @param apiIdentifier
	 * @param applicationId
	 * @param token
	 * @throws Exception
	 */
	void subscribe(String apiIdentifier, String applicationId, String token) throws Exception;
	/**
	 * Unsubscribe from an API 
	 * @param subscriptionId
	 * @param token
	 * @throws Exception
	 */
	void unsubscribe(String subscriptionId, String token) throws Exception;

	/**
	 * Get application ID for the specified app name
	 * @param name
	 * @param token
	 * @return
	 * @throws Exception
	 */
	String getApplicationId(String name, String token) throws Exception;

	/**
	 * @param domain
	 * @param email
	 * @param password
	 * @param name
	 * @param surname
	 * @throws Exception
	 */
	void createPublisher(String domain, String email, String password, String name, String surname) throws Exception;
	
	/**
	 * @param email
	 * @param password
	 * @param name
	 * @param surname
	 * @throws Exception
	 */
	void createUser(String email, String password, String name, String surname) throws Exception;

	/**
	 * @param email
	 * @param context
	 * @param newPassword
	 */
	void updatePublisherPassword(String email, String context, String newPassword)  throws Exception;

	/**
	 * @param email
	 * @param newPassword
	 */
	void updatePassword(String email, String newPassword) throws Exception;


}
