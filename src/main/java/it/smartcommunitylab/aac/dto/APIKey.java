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

package it.smartcommunitylab.aac.dto;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.model.APIKeyEntity;
import it.smartcommunitylab.aac.model.Role;

/**
 * DB entity storing the client app information
 * @author raman
 *
 */
public class APIKey {

	private static final ObjectMapper objectMapper = new ObjectMapper();
	
	private String apiKey;
	private String clientId;
	private Map<String,Object> additionalInformation;
	private Long userId;
	private String username;
	private Set<String> scope;
	private Long validity;
	private long issuedTime;

	private Set<Role> roles;
	
	public APIKey() {
		super();
	}

	@SuppressWarnings("unchecked")
	public APIKey(APIKeyEntity entity) {
		super();
		this.apiKey = entity.getApiKey();
		this.clientId = entity.getClientId();
		if (entity.getAdditionalInformation() != null) {
			this.additionalInformation = objectMapper.convertValue(entity.getAdditionalInformation(), Map.class);
		}
		this.userId = entity.getUserId();
		this.username = entity.getUsername();
		this.validity = entity.getValidity();
		this.issuedTime = entity.getIssuedTime();
		if (entity.getScope() != null) {
			this.scope = StringUtils.commaDelimitedListToSet(entity.getScope()); 
		}
		if (entity.getRoles() != null) {
			TypeReference<Role> tr = new TypeReference<Role>() {};
			try {
				this.roles = objectMapper.readValue(entity.getRoles(), tr);
			} catch (Exception e) {
				this.roles = Collections.emptySet();
			} 
		}
	}


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

	public Map<String,Object> getAdditionalInformation() {
		return additionalInformation;
	}

	public void setAdditionalInformation(Map<String,Object> additionalInformation) {
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

	public Set<String> getScope() {
		return scope;
	}

	public void setScope(Set<String> scope) {
		this.scope = scope;
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

	/**
	 * @param data
	 * @return
	 */
	public static String toDataString(Map<String, Object> data) {
		if (data == null) return null;
		try {
			return objectMapper.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	/**
	 * @param userRolesByClient
	 * @return
	 */
	public static String toRolesString(Set<Role> userRolesByClient) {
		try {
			return objectMapper.writeValueAsString(userRolesByClient);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @return
	 */
	public boolean hasExpired() {
		if (getValidity() != null && getValidity() > 0) {
			return getIssuedTime() + getValidity() > System.currentTimeMillis();
		}
		return false;
	}

}
