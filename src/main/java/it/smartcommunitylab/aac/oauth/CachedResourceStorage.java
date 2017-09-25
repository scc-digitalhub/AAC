/**
 *    Copyright 2012-2013 Trento RISE
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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import it.smartcommunitylab.aac.model.Resource;
import it.smartcommunitylab.aac.repository.ResourceRepository;

/**
 * Implementation of the resource storage with in-memory cache of resource model.
 * 
 * @author raman
 *
 */
@Transactional
public class CachedResourceStorage implements ResourceStorage {

	@Autowired
	private ResourceRepository resourceRepository;
	
	private ConcurrentMap<String, Resource> resources = new ConcurrentHashMap<String, Resource>();
	private ConcurrentMap<String, Resource> resourcesIdMap = new ConcurrentHashMap<String, Resource>();

	/**
	 * Update the in-memory map in case of resource repository changes
	 */
	@Transactional
	private void reset() {
		synchronized (resourcesIdMap) {
			resourcesIdMap.clear();
			resources.clear();
			for (Resource resource : resourceRepository.findAll()) {
				resources.put(resource.getResourceUri(), resource);
				resourcesIdMap.put(resource.getResourceId().toString(), resource);
			}
		}
	}

	/**
	 * Retrieve the cached copy of the specified resource 
	 * @param resourceUri
	 * @return
	 */
	@Override
	public Resource loadResourceByResourceUri(String resourceUri) {
		if (resources.isEmpty()) {
			reset();
		}
		return resources.get(resourceUri);
	}
	
	@Override
	public Resource storeResource(Resource resource) {
		if (resource != null) {
			Resource old = resourceRepository.findByResourceUri(resource.getResourceUri());
			if (old != null) {
				resource.setResourceId(old.getResourceId());
			}
			Resource res = resourceRepository.save(resource);
			reset();
			return res;
		}
		return null;
	}

	@Override
	public void storeResources(List<Resource> resources) {
		if (resources != null) {
			for (Resource resource : resources) {
				storeResource(resource);
			}
			reset();
		}
	}
	
	
}
