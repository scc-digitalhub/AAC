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

package it.smartcommunitylab.aac.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import it.smartcommunitylab.aac.Config.AUTHORITY;

/**
 * @author raman
 *
 */
@Entity
@Table(name="service_scope")
public class ServiceScope {

	@Id
	private String scope;

	/**
	 * ServiceDescriptor ID
	 */
	@ManyToOne(optional=false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(nullable=false, name="serviceId",referencedColumnName="serviceId")
	private Service service;

	/**
	 * Human-readable scope name
	 */
	private String name;
	/**
	 * Resource description
	 */
	private String description;

	/**
	 * Claims exposed with the scopes (comma-separated list)
	 */
	@Column(columnDefinition="LONGTEXT")
	private String claims;

	/**
	 * Roles required to access the scope
	 */
	@Column(columnDefinition="LONGTEXT")
	private String roles;

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
	 * @return the scope
	 */
	public String getScope() {
		return scope;
	}

	/**
	 * @param scope the scope to set
	 */
	public void setScope(String scope) {
		this.scope = scope;
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
	 * @return the claims
	 */
	public String getClaims() {
		return claims;
	}

	/**
	 * @param claims the claims to set
	 */
	public void setClaims(String claims) {
		this.claims = claims;
	}

	/**
	 * @return the roles
	 */
	public String getRoles() {
		return roles;
	}

	/**
	 * @param roles the roles to set
	 */
	public void setRoles(String roles) {
		this.roles = roles;
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

	public boolean isApprovalRequired() {
		return approvalRequired;
	}

	public void setApprovalRequired(boolean approvalRequired) {
		this.approvalRequired = approvalRequired;
	} 

}
