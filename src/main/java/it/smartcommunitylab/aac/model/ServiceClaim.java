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

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import it.smartcommunitylab.aac.Config.CLAIM_TYPE;

/**
 * @author raman
 *
 */
@Entity
@Table(name="service_claim")
public class ServiceClaim {

	@Id
	@GeneratedValue
	private Long claimId;
	
	private String claim;
	
	/**
	 * ServiceDescriptor ID
	 */
	@ManyToOne(optional=false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(nullable=false, name="serviceId",referencedColumnName="serviceId")
	private Service service;

	/**
	 * Human-readable claim name
	 */
	private String name;

	/**
	 * If claim type is multiple
	 */
	private boolean multiple;
	
	@Enumerated(EnumType.STRING)
	private CLAIM_TYPE type;

	/**
	 * @return the claimId
	 */
	public Long getClaimId() {
		return claimId;
	}

	/**
	 * @param claimId the claimId to set
	 */
	public void setClaimId(Long claimId) {
		this.claimId = claimId;
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
	 * @return the multiple
	 */
	public boolean isMultiple() {
		return multiple;
	}

	/**
	 * @param multiple the multiple to set
	 */
	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
	}

	/**
	 * @return the type
	 */
	public CLAIM_TYPE getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(CLAIM_TYPE type) {
		this.type = type;
	}

	public String getClaim() {
		return claim;
	}

	public void setClaim(String claim) {
		this.claim = claim;
	}
}
