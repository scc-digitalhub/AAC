/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
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

package it.smartcommunitylab.aac.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author raman
 *
 */
@Entity
@Table(name="service")
public class ServiceDescriptor {

	@Id
	private String serviceId;
	
	private String serviceName;
	private String description;
	
	@Column(columnDefinition="LONGTEXT")
	private String resourceDefinitions;
	@Column(columnDefinition="LONGTEXT")
	private String resourceMappings;
	
	private String ownerId;
	
	public String getServiceId() {
		return serviceId;
	}
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getResourceDefinitions() {
		return resourceDefinitions;
	}
	public void setResourceDefinitions(String resourceDefinitions) {
		this.resourceDefinitions = resourceDefinitions;
	}
	public String getResourceMappings() {
		return resourceMappings;
	}
	public void setResourceMappings(String resourceMappings) {
		this.resourceMappings = resourceMappings;
	}
	/**
	 * @return the ownerId
	 */
	public String getOwnerId() {
		return ownerId;
	}
	/**
	 * @param ownerId the ownerId to set
	 */
	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
}
