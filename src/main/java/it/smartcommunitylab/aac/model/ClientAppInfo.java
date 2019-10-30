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

import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Additional app info descriptor
 * @author raman
 *
 */
public class ClientAppInfo {
	
	public static final int APPROVED = 1;
	public static final int REJECTED = 2;
	public static final int REQUESTED = 0;
	public static final Integer UNKNOWN = -1;


	private static ObjectMapper mapper = new ObjectMapper();
	
	private String name, displayName;

	private Map<String, Boolean> resourceApprovals;
	
	private Map<String, Integer> identityProviders;
	
	private String scope;
	
	private Map<String, Map<String, Object>> providerConfigurations;
	
	private Set<String> uniqueSpaces;
	private String claimMapping; 
	
	static {
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	public static ClientAppInfo convert(Map<String,Object> map) {
		return mapper.convertValue(map, ClientAppInfo.class);
	}

	public String toJson() throws Exception {
		return mapper.writeValueAsString(this);
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
	 * @return the resourceApprovals
	 */
	public Map<String, Boolean> getResourceApprovals() {
		return resourceApprovals;
	}

	/**
	 * @param resourceApprovals the resourceApprovals to set
	 */
	public void setResourceApprovals(Map<String, Boolean> resourceApprovals) {
		this.resourceApprovals = resourceApprovals;
	}

	/**
	 * @return the identityProviders
	 */
	public Map<String, Integer> getIdentityProviders() {
		return identityProviders;
	}

	/**
	 * @param identityProviders the identityProviders to set
	 */
	public void setIdentityProviders(Map<String, Integer> identityProviders) {
		this.identityProviders = identityProviders;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public Map<String, Map<String, Object>> getProviderConfigurations() {
		return providerConfigurations;
	}

	public void setProviderConfigurations(Map<String, Map<String, Object>> providerConfigurations) {
		this.providerConfigurations = providerConfigurations;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @return the uniqueSpaces
	 */
	public Set<String> getUniqueSpaces() {
		return uniqueSpaces;
	}

	/**
	 * @param uniqueSpaces the uniqueSpaces to set
	 */
	public void setUniqueSpaces(Set<String> uniqueSpaces) {
		this.uniqueSpaces = uniqueSpaces;
	}

	/**
	 * @return the claimMapping
	 */
	public String getClaimMapping() {
		return claimMapping;
	}

	/**
	 * @param claimMapping the claimMapping to set
	 */
	public void setClaimMapping(String claimMapping) {
		this.claimMapping = claimMapping;
	}
}
