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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * DB entity storing the client app information
 * @author raman
 *
 */
@Entity
@Table(name = "api_key")
public class APIKeyEntity {

	@Id
	@Column(name = "api_key")
	private String apiKey;
	
	@Column(name = "client_id",nullable = false)
	private String clientId;

	@Column(name = "additional_information",columnDefinition="LONGTEXT")
	private String additionalInformation;

	@Column(name = "user_id",nullable = false)
	private Long userId;
	
	@Column(nullable = false)
	private String username;
	
	@Column(name = "scope",columnDefinition="LONGTEXT")
	private String scope;
	@Column(name = "roles",columnDefinition="LONGTEXT")
	private String roles;

	@Column(name = "validity")
	private Long validity;
	
	@Column(name = "issued_time",nullable = false)
	private long issuedTime;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getAdditionalInformation() {
		return additionalInformation;
	}

	public void setAdditionalInformation(String additionalInformation) {
		this.additionalInformation = additionalInformation;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}
	public String getRoles() {
		return roles;
	}

	public void setRoles(String roles) {
		this.roles = roles;
	}

	public Long getValidity() {
		return validity;
	}

	public void setValidity(Long validity) {
		this.validity = validity;
	}

	public long getIssuedTime() {
		return issuedTime;
	}

	public void setIssuedTime(long issuedTime) {
		this.issuedTime = issuedTime;
	}
}
