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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.wso2.carbon.um.ws.api.stub.ClaimValue;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import it.smartcommunitylab.aac.apimanager.model.AACAPI;
import it.smartcommunitylab.aac.apimanager.model.DataList;
import it.smartcommunitylab.aac.apimanager.model.Subscription;
import it.smartcommunitylab.aac.wso2.model.API;
import it.smartcommunitylab.aac.wso2.model.APIInfo;
import it.smartcommunitylab.aac.wso2.model.App;
import it.smartcommunitylab.aac.wso2.model.CorsConfiguration;
import it.smartcommunitylab.aac.wso2.services.APIPublisherService;
import it.smartcommunitylab.aac.wso2.services.APIStoreService;
import it.smartcommunitylab.aac.wso2.services.UserManagementService;
import it.smartcommunitylab.aac.wso2.services.Utils;

/**
 * @author raman
 *
 */
public class WSO2APIManager implements APIManager {

	private final static String ENDPOINT_CONFIG = "{\"production_endpoints\":{\"url\":\"${application.internalUrl}\",\"config\":null},\"sandbox_endpoints\":{\"url\":\"${application.internalUrl}\",\"config\":null},\"endpoint_type\":\"http\"}";

	@Autowired
	private ConfigurableEnvironment env;

	@Autowired
	private APIPublisherService pub;

	@Autowired
	private APIStoreService sub;
	
	@Autowired
	private UserManagementService umService;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void publishAPI(String jsonModel, String name, String description, String context, String token) throws Exception {
		Map apis = pub.findAPI(name, token);

		List list = (List) apis.get("list");
		// System.err.println(apis);
		if (list.isEmpty() || !list.stream().anyMatch(x -> name.equals(((Map) x).get("name")))) {
			String swagger = Resources.toString(Resources.getResource(jsonModel), Charsets.UTF_8);
			swagger = env.resolvePlaceholders(swagger);

			API api = new API();

			api.setName(name);
			api.setDescription(description);
			api.setContext(context);
			api.setVersion("1.0.0");
			api.setProvider("admin");
			api.setApiDefinition(swagger);
			api.setStatus("CREATED");
			api.setVisibility("PUBLIC");
			api.setSubscriptionAvailability("all_tenants");
			api.setIsDefaultVersion(true);
			api.setEndpointConfig(env.resolvePlaceholders(ENDPOINT_CONFIG));

			CorsConfiguration cors = new CorsConfiguration();
			cors.setCorsConfigurationEnabled(true);
			cors.setAccessControlAllowOrigins(Lists.newArrayList("*"));
			cors.setAccessControlAllowHeaders(
					Lists.newArrayList("authorization", "Access-Control-Allow-Origin", "Content-Type", "SOAPAction"));
			cors.setAccessControlAllowMethods(Lists.newArrayList("GET", "PUT", "POST", "DELETE", "PATCH", "OPTIONS"));
			api.setCorsConfiguration(cors);

			API result = pub.publishAPI(api, token);
			pub.changeAPIStatus(result.getId(), "Publish", token);

			// System.err.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
		}		
	}


	/* (non-Javadoc)
	 * @see it.smartcommunitylab.aac.apimanager.APIManager#getAPIs(java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String)
	 */
	@Override
	public DataList<AACAPI> getAPIs(Integer offset, Integer limit, String query,
			String token) throws Exception {
		return convertAPIDataList(pub.getAPIs(offset, limit, query, token));
	}


	/**
	 * @param apIs
	 * @return
	 */
	private DataList<AACAPI> convertAPIDataList(it.smartcommunitylab.aac.wso2.model.DataList<APIInfo> apis) {
		DataList<AACAPI> res = new DataList<>();
		res.setCount(apis.getCount());
		
		List<AACAPI> list = new LinkedList<>();
		if (apis.getList() != null) {
			apis.getList().forEach(a -> list.add(convertAPIInfo(a)));
		}
		res.setList(list);
		return res;
	}


	/* (non-Javadoc)
	 * @see it.smartcommunitylab.aac.apimanager.APIManager#getAPI(java.lang.String)
	 */
	@Override
	public AACAPI getAPI(String apiId, String token) throws Exception {
		API api = pub.getAPI(apiId, token);
		
		if (api.getThumbnailUri() != null) {
			api.setThumbnailUri("/mgmt"+api.getThumbnailUri());
		}
		AACAPI result = convertAPI(api);
		return result;
	}


	private AACAPI convertAPI(API api) {
		AACAPI res = convertAPIInfo(api);
		if (api.getThumbnailUri() != null) {
			api.setThumbnailUri("/mgmt"+api.getThumbnailUri());
		}
		return res;
	}
	private AACAPI convertAPIInfo(APIInfo api) {
		AACAPI res = new AACAPI();
		res.setContext(api.getContext());
		res.setDescription(api.getDescription());
		res.setId(api.getId());
		res.setName(api.getName());
		res.setProvider(api.getProvider());
		res.setStatus(api.getStatus());
		res.setVersion(api.getVersion());
		return res;
	}

	/* (non-Javadoc)
	 * @see it.smartcommunitylab.aac.apimanager.APIManager#getAPIThumbnail(java.lang.String, java.lang.String)
	 */
	@Override
	public byte[] getAPIThumbnail(String apiId, String token) {
		return pub.getAPIThumbnail(apiId, token);
	}


	/* (non-Javadoc)
	 * @see it.smartcommunitylab.aac.apimanager.APIManager#getSubscriptions(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.String)
	 */
	@Override
	public DataList<Subscription> getSubscriptions(String apiId, String tenant, Integer offset, Integer limit, String token) {
		it.smartcommunitylab.aac.wso2.model.DataList<it.smartcommunitylab.aac.wso2.model.Subscription> subs = pub.getSubscriptions(apiId, tenant, offset, limit, token);
		DataList<Subscription> res = new DataList<>();
		res.setList(new LinkedList<>());	
		res.setCount(subs.getCount());
		if (subs.getList() != null) {
			subs.getList().forEach(s -> {
				Subscription ns = convertSubscription(s);
				res.getList().add(ns);
			});
		}
		
		return res;
	}


	protected Subscription convertSubscription(it.smartcommunitylab.aac.wso2.model.Subscription s) {
		Subscription ns = new Subscription();
		ns.setApiIdentifier(s.getApiIdentifier());
		ns.setApplicationId(s.getApplicationId());
		ns.setAppName(s.getAppName());
		ns.setRoles(s.getRoles());
		ns.setStatus(s.getStatus());
		ns.setSubscriber(s.getSubscriber());
		ns.setSubscriptionId(s.getSubscriptionId());
		ns.setTier(s.getTier());
		return ns;
	}


	/* (non-Javadoc)
	 * @see it.smartcommunitylab.aac.apimanager.APIManager#getSubscriptions(java.lang.String, java.lang.String)
	 */
	@Override
	public List<Subscription> getSubscriptions(String applicationName, String token) {
		List<it.smartcommunitylab.aac.wso2.model.Subscription> subs = sub.getSubscriptions(applicationName, token);
		List<Subscription> list = new LinkedList<>();
		if (subs != null) {
			subs.forEach(s -> list.add(convertSubscription(s)));
		}
		return list;
	}


	/* (non-Javadoc)
	 * @see it.smartcommunitylab.aac.apimanager.APIManager#findAPI(java.lang.String, java.lang.String)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public DataList<AACAPI> findAPI(String name, String token) throws Exception {
		Map map1 = pub.findAPI(name, token);
		DataList<AACAPI> res = new DataList<>();
		res.setList(new LinkedList<>());
		List<Map> list = (List<Map>)map1.get("list");
		for (Map map2: list) {
			AACAPI api = new AACAPI();
			api.setContext((String)map2.get("context"));
			api.setId((String) map2.get("id"));
			api.setName((String) map2.get("name"));
			api.setVersion((String) map2.get("version"));
			api.setStatus((String) map2.get("status"));
			api.setDescription((String) map2.get("description"));
			api.setProvider((String) map2.get("provider"));
			res.getList().add(api);
		}
		return res;
	}


	/* (non-Javadoc)
	 * @see it.smartcommunitylab.aac.apimanager.APIManager#subscribe(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void subscribe(String apiIdentifier, String applicationId, String token) throws Exception {
		sub.subscribe(apiIdentifier, applicationId, token);
	}


	/* (non-Javadoc)
	 * @see it.smartcommunitylab.aac.apimanager.APIManager#unsubscribe(java.lang.String, java.lang.String)
	 */
	@Override
	public void unsubscribe(String subscriptionId, String token) throws Exception {
		sub.unsubscribe(subscriptionId, token);
	}


	/* (non-Javadoc)
	 * @see it.smartcommunitylab.aac.apimanager.APIManager#getApplicationId(java.lang.String, java.lang.String)
	 */
	@Override
	public String getApplicationId(String name, String token) throws Exception {
		App app = sub.getApplication(name, token);
		String applicationId = app.getApplicationId();
		return applicationId;
	}


	/* (non-Javadoc)
	 * @see it.smartcommunitylab.aac.apimanager.APIManager#createPublisher(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void createPublisher(String domain, String email, String password, String name, String surname) throws Exception {
		umService.createPublisher(domain, email, password, name, surname);
	}


	/* (non-Javadoc)
	 * @see it.smartcommunitylab.aac.apimanager.APIManager#createUser(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void createUser(String email, String password, String name, String surname) throws Exception {
		ClaimValue[] claims = new ClaimValue[] {
				Utils.createClaimValue("http://wso2.org/claims/emailaddress", email),
				Utils.createClaimValue("http://wso2.org/claims/givenname", name),
				Utils.createClaimValue("http://wso2.org/claims/lastname", surname) 
		};
		umService.createSubscriber(email, password, claims);
	}


	/* (non-Javadoc)
	 * @see it.smartcommunitylab.aac.apimanager.APIManager#updatePublisherPassword(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void updatePublisherPassword(String email, String context, String newPassword)  throws Exception{
		umService.updatePublisherPassword(email, context, newPassword);	
		
	}


	/* (non-Javadoc)
	 * @see it.smartcommunitylab.aac.apimanager.APIManager#updatePassword(java.lang.String, java.lang.String)
	 */
	@Override
	public void updatePassword(String email, String newPassword) throws Exception {
		umService.updateNormalUserPassword(email, newPassword);	
	}


}
