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

package it.smartcommunitylab.aac.scim.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wso2.charon3.core.config.CharonConfiguration;
import org.wso2.charon3.core.exceptions.NotFoundException;
import org.wso2.charon3.core.extensions.UserManager;
import org.wso2.charon3.core.protocol.endpoints.AbstractResourceManager;
import org.wso2.charon3.core.protocol.endpoints.GroupResourceManager;
import org.wso2.charon3.core.protocol.endpoints.ResourceURLBuilder;
import org.wso2.charon3.core.protocol.endpoints.UserResourceManager;

import it.smartcommunitylab.aac.group.GroupManager;

/**
 * @author raman
 *
 */
@Service
public class SCIMTenantAwareUserManager {

	private static final Map<String, UserManager> managers = Collections.synchronizedMap(new HashMap<>());
	private static final Map<String, UserResourceManager> resourceManagers = Collections.synchronizedMap(new HashMap<>());
	private static final Map<String, GroupResourceManager> groupResourceManagers = Collections.synchronizedMap(new HashMap<>());
	
	
	@Autowired
	private it.smartcommunitylab.aac.core.UserManager userManager;
	@Autowired
	private GroupManager groupManager;

	@Value("${application.url:}")
	private String applicationUrl;

	@PostConstruct
	public void init() {
		AbstractResourceManager.setResourceURLBuilder(resourceURLBuilder);
        
		// configuration
		ArrayList<Object[]> authSchemes = new ArrayList<>();
		authSchemes.add(new Object[] {"OAuth Bearer Token", "Authentication scheme using the OAuth Bearer Token Standard", "https://datatracker.ietf.org/doc/html/rfc6750", "https://datatracker.ietf.org/doc/html/rfc6750", "oauthbearertoken", true});
		CharonConfiguration.getInstance().setAuthenticationSchemes(authSchemes);
        CharonConfiguration.getInstance().setBulkSupport(false, 0, 0);
        CharonConfiguration.getInstance().setChangePasswordSupport(false);
        CharonConfiguration.getInstance().setCountValueForPagination(20);
        CharonConfiguration.getInstance().setETagSupport(false);
        CharonConfiguration.getInstance().setFilterSupport(true, 20);
        CharonConfiguration.getInstance().setPatchSupport(false);
        CharonConfiguration.getInstance().setSortSupport(true);
	}
	
	/**
	 * @param realm
	 * @return
	 */
	public UserManager getUserManager(String realm) {
		// Thread local to overcome the problem of static location builder
		REALM_CONTAINER.set(realm);
		UserManager manager = managers.get(realm);
		if (manager == null) {
			manager = new SCIMUserManager(realm, userManager, groupManager, applicationUrl);
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

	/**
	 * @param realm
	 * @return
	 */
	public GroupResourceManager getGroupResourceManager(String realm) {
		GroupResourceManager manager = groupResourceManagers.get(realm);
		if (manager == null) {
			manager = new GroupResourceManager();
			groupResourceManagers.put(realm, manager);
		}
		return manager;
	}
	
	private static ThreadLocal<String> REALM_CONTAINER = new ThreadLocal<String>() {
	     @Override
	     protected String initialValue() {
	             return "";
	     }
    };	

    private ResourceURLBuilder resourceURLBuilder = new ResourceURLBuilder() {
		
		@Override
		public String build(String resource) throws NotFoundException {
			return (applicationUrl.endsWith("/") ? applicationUrl.substring(0, applicationUrl.length()-1) : applicationUrl) + "/scim/v2/" + REALM_CONTAINER.get() + resource;
		}
	};
    
}
