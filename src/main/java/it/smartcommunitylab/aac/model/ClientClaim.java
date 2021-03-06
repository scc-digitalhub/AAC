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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * @author raman
 *
 */
@Entity
@Table(name="client_claim")
public class ClientClaim {

	@Id
	@GeneratedValue
	private Long id;

	@Column(columnDefinition="LONGTEXT")
	private String value;
	
	/**
	 * ServiceClaim ID
	 */
	@ManyToOne(optional=false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(nullable=false, name="claimId",referencedColumnName="claimId")
	private ServiceClaim claim;

	/**
	 * ServiceClaim ID
	 */
	@ManyToOne(optional=false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(nullable=false, name="clientId",referencedColumnName="client_id")
	private ClientDetailsEntity client;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public ServiceClaim getClaim() {
		return claim;
	}

	public void setClaim(ServiceClaim claim) {
		this.claim = claim;
	}

	public ClientDetailsEntity getClient() {
		return client;
	}

	public void setClient(ClientDetailsEntity client) {
		this.client = client;
	}

}
