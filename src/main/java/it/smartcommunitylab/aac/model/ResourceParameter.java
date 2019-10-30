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

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import it.smartcommunitylab.aac.Config.RESOURCE_VISIBILITY;

/**
 *
 * DB entity for resource parameters
 * @author raman
 *
 */
@Entity
@Table(name="resource_parameter",
       uniqueConstraints={@UniqueConstraint(columnNames={"parameter", "value", "serviceid"})})
public class ResourceParameter {
	@Id
	@GeneratedValue
	private Long id;
	
	private String parameter;
	/**
	 * ServiceDescriptor
	 */
	@ManyToOne(optional=false)
	@JoinColumn(nullable=false, name="serviceid",referencedColumnName="serviceId")
	private ServiceDescriptor service;
	/**
	 * parameter value
	 */
	private String value;
	/**
	 * Owning client app
	 */
	private String clientId;
	/**
	 * Visibility of the resource parameter for the other apps
	 */
	@Enumerated(EnumType.STRING)
	private RESOURCE_VISIBILITY visibility = RESOURCE_VISIBILITY.CLIENT_APP;

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return the parameter
	 */
	public String getParameter() {
		return parameter;
	}
	/**
	 * @param parameter the parameter to set
	 */
	public void setParameter(String parameter) {
		this.parameter = parameter;
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
	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
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
}
