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

package it.smartcommunitylab.aac.model;

import java.util.List;
import java.util.Map;

import it.smartcommunitylab.aac.jaxbmodel.Service;

/**
 * Permissions descriptor with client app own resource properties, services, resources available,
 * resource approval statues, and resources selected by the client
 * @author raman
 *
 */
public class Permissions {

	private Map<String,List<ResourceParameter>> ownResources;
	private Service service;
	private Map<String, List<Resource>> availableResources;
	private Map<String,Boolean> selectedResources;
	
	private Map<String,Integer> resourceApprovals;
	/**
	 * @return the ownResources
	 */
	public Map<String, List<ResourceParameter>> getOwnResources() {
		return ownResources;
	}
	/**
	 * @param ownResources the ownResources to set
	 */
	public void setOwnResources(Map<String, List<ResourceParameter>> ownResources) {
		this.ownResources = ownResources;
	}
	/**
	 * @return the service
	 */
	public Service getService() {
		return service;
	}
	/**
	 * @param service the service to set
	 */
	public void setService(Service service) {
		this.service = service;
	}
	/**
	 * @return the selectedResources
	 */
	public Map<String, Boolean> getSelectedResources() {
		return selectedResources;
	}
	/**
	 * @return the availableResources
	 */
	public Map<String, List<Resource>> getAvailableResources() {
		return availableResources;
	}
	/**
	 * @param availableResources the availableResources to set
	 */
	public void setAvailableResources(
			Map<String, List<Resource>> availableResources) {
		this.availableResources = availableResources;
	}
	/**
	 * @param selectedResources the selectedResources to set
	 */
	public void setSelectedResources(Map<String, Boolean> selectedResources) {
		this.selectedResources = selectedResources;
	}
	/**
	 * @return the resourceApprovals
	 */
	public Map<String, Integer> getResourceApprovals() {
		return resourceApprovals;
	}
	/**
	 * @param resourceApprovals the resourceApprovals to set
	 */
	public void setResourceApprovals(Map<String, Integer> resourceApprovals) {
		this.resourceApprovals = resourceApprovals;
	}
}
