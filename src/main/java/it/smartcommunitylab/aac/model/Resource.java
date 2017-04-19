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
package it.smartcommunitylab.aac.model;

import it.smartcommunitylab.aac.Config.AUTHORITY;
import it.smartcommunitylab.aac.Config.RESOURCE_VISIBILITY;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * DB entity to store resource data 
 * @author raman
 *
 */
@Entity
@Table(name="resource")
public class Resource {
	@Id
	@GeneratedValue
	private Long resourceId;
	/**
	 * Resource category used to group different resources
	 */
	private String resourceType;
	/**
	 * Resource-specific symbolic name
	 */
	private String resourceUri;
	
	/**
	 * Human-readable resource name
	 */
	private String name;
	/**
	 * Resource description
	 */
	private String description;
	/**
	 * Application that created this resource (if applicable)
	 */
	private String clientId;
	/**
	 * Authority that can access this resource
	 */
	@Enumerated(EnumType.STRING)
	private AUTHORITY authority; 

	/**
	 * Whether explicit manual approval required
	 */
	private boolean approvalRequired = false;

	/**
	 * Whether non-owning clients can request and access this resource
	 */
	private boolean accessibleByOthers = true;
	
	/**
	 * Visibility of the resource for the other apps
	 */
	@Enumerated(EnumType.STRING)
	private RESOURCE_VISIBILITY visibility = RESOURCE_VISIBILITY.CLIENT_APP;
	
	/**
	 * Reference to the resource Parameter
	 */
	@ManyToOne(optional=true)
	@JoinColumn(nullable=true, name="resourceparameterid",referencedColumnName="id")
	private ResourceParameter resourceParameter;
	
	/**
	 * ServiceDescriptor ID
	 */
	@ManyToOne(optional=false)
	@JoinColumn(nullable=false, name="serviceid",referencedColumnName="serviceId")
	private ServiceDescriptor service;
	
	/**
	 * WSO2 roles
	 */
	private String roles;
	
	/**
	 * @return the resourceId
	 */
	public Long getResourceId() {
		return resourceId;
	}

	/**
	 * @param resourceId the resourceId to set
	 */
	public void setResourceId(Long resourceId) {
		this.resourceId = resourceId;
	}

	/**
	 * @return the resourceType
	 */
	public String getResourceType() {
		return resourceType;
	}

	/**
	 * @param resourceType the resourceType to set
	 */
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	/**
	 * @return the resourceUri
	 */
	public String getResourceUri() {
		return resourceUri;
	}

	/**
	 * @param resourceUri the resourceUri to set
	 */
	public void setResourceUri(String resourceUri) {
		this.resourceUri = resourceUri;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the clientId
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * @param clientId the clientId to set
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * @return the authority
	 */
	public AUTHORITY getAuthority() {
		return authority;
	}

	/**
	 * @param authority the authority to set
	 */
	public void setAuthority(AUTHORITY authority) {
		this.authority = authority;
	}

	/**
	 * @return the approvalRequired
	 */
	public boolean isApprovalRequired() {
		return approvalRequired;
	}

	/**
	 * @param approvalRequired the approvalRequired to set
	 */
	public void setApprovalRequired(boolean approvalRequired) {
		this.approvalRequired = approvalRequired;
	}

	/**
	 * @return the accessibleByOthers
	 */
	public boolean isAccessibleByOthers() {
		return accessibleByOthers;
	}

	/**
	 * @param accessibleByOthers the accessibleByClient to set
	 */
	public void setAccessibleByOthers(boolean accessibleByOthers) {
		this.accessibleByOthers = accessibleByOthers;
	}

	/**
	 * @return the visibility
	 */
	public RESOURCE_VISIBILITY getVisibility() {
		return visibility;
	}

	/**
	 * @param visibility the visibility to set
	 */
	public void setVisibility(RESOURCE_VISIBILITY visibility) {
		this.visibility = visibility;
	}

	/**
	 * @return the resourceParameter
	 */
	public ResourceParameter getResourceParameter() {
		return resourceParameter;
	}

	/**
	 * @param resourceParameter the resourceParameter to set
	 */
	public void setResourceParameter(ResourceParameter resourceParameter) {
		this.resourceParameter = resourceParameter;
	}

	/**
	 * @return the service
	 */
	public ServiceDescriptor getService() {
		return service;
	}

	/**
	 * @param service the service to set
	 */
	public void setService(ServiceDescriptor service) {
		this.service = service;
	}

	public String getRoles() {
		return roles;
	}

	public void setRoles(String roles) {
		this.roles = roles;
	}
	
}
