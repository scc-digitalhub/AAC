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

package it.smartcommunitylab.aac.oauth;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import it.smartcommunitylab.aac.manager.ServiceManager;
import it.smartcommunitylab.aac.model.ServiceScope;

/**
 * Implementation of the scope storage with in-memory cache of scope model.
 * 
 * @author raman
 *
 */
@Transactional
public class CachedServiceScopeServices implements ServiceScopeServices {

	@Autowired
	private ServiceManager serviceManager;	
	
	private ConcurrentMap<String, ServiceScope> resources = new ConcurrentHashMap<String, ServiceScope>();
	private ConcurrentMap<String, ServiceScope> resourcesIdMap = new ConcurrentHashMap<String, ServiceScope>();

	/**
	 * Update the in-memory map in case of resource repository changes
	 */
	@Transactional
	private void reset() {
		synchronized (resourcesIdMap) {
			resourcesIdMap.clear();
			resources.clear();
			for (ServiceScope scope : serviceManager.findAllScopes()) {
				resources.put(scope.getScope(), scope);
				resourcesIdMap.put(scope.getScope().toString(), scope);
			}
		}
	}

	/**
	 * Retrieve the cached copy of the specified resource 
	 * @param resourceUri
	 * @return
	 */
	@Override
	public ServiceScope loadScope(String resourceUri) {
		if (resources.isEmpty()) {
			reset();
		}
		return resources.get(resourceUri);
	}
	
}
