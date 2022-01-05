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

package it.smartcommunitylab.aac.scim.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wso2.charon3.core.attributes.ComplexAttribute;
import org.wso2.charon3.core.attributes.MultiValuedAttribute;
import org.wso2.charon3.core.extensions.UserManager;
import org.wso2.charon3.core.protocol.endpoints.UserResourceManager;
import org.wso2.charon3.core.schema.SCIMConstants;
import org.wso2.charon3.core.schema.SCIMDefinitions.DataType;
import org.wso2.charon3.core.schema.SCIMResourceSchemaManager;

import it.smartcommunitylab.aac.core.service.UserService;

/**
 * @author raman
 *
 */
@Service
public class SCIMTenantAwareUserManager {

	private static final Map<String, UserManager> managers = Collections.synchronizedMap(new HashMap<>());
	private static final Map<String, UserResourceManager> resourceManagers = Collections.synchronizedMap(new HashMap<>());
	
	
	@Autowired
	private UserService userService;

	@PostConstruct
	public void init() {
		Map<String, String> endpointURLs = new HashMap<>();
		endpointURLs.put(SCIMConstants.USER_ENDPOINT, SCIMConstants.USER_ENDPOINT);
        endpointURLs.put(SCIMConstants.GROUP_ENDPOINT, SCIMConstants.USER_ENDPOINT);
        UserResourceManager.setEndpointURLMap(endpointURLs);
	}
	
	/**
	 * @param realm
	 * @return
	 */
	public UserManager getUserManager(String realm) {
		UserManager manager = managers.get(realm);
		if (manager == null) {
			manager = new SCIMUserManager(realm, userService);
			managers.put(realm, manager);
		}
		return manager;
	}

	/**
	 * @param realm
	 * @return
	 */
	public UserResourceManager getUserResourceManager(String realm) {
		UserResourceManager manager = resourceManagers.get(realm);
		if (manager == null) {
			manager = new UserResourceManager();
			resourceManagers.put(realm, manager);
		}
		return manager;
	}

}
