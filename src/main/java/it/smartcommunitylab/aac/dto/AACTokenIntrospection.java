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

package it.smartcommunitylab.aac.dto;

/**
 * @author raman
 *
 */
public class AACTokenIntrospection extends TokenIntrospection {
	private String aac_user_id;
	private String aac_grantType;
	private Boolean aac_applicationToken;
	private String aac_am_tenant;
	
	/**
	 * @return the aac_user_id
	 */
	public String getAac_user_id() {
		return aac_user_id;
	}
	/**
	 * @param aac_user_id the aac_user_id to set
	 */
	public void setAac_user_id(String aac_user_id) {
		this.aac_user_id = aac_user_id;
	}
	/**
	 * @return the aac_grantType
	 */
	public String getAac_grantType() {
		return aac_grantType;
	}
	/**
	 * @param aac_grantType the aac_grantType to set
	 */
	public void setAac_grantType(String aac_grantType) {
		this.aac_grantType = aac_grantType;
	}
	/**
	 * @return the aac_applicationToken
	 */
	public Boolean getAac_applicationToken() {
		return aac_applicationToken;
	}
	/**
	 * @param aac_applicationToken the aac_applicationToken to set
	 */
	public void setAac_applicationToken(Boolean aac_applicationToken) {
		this.aac_applicationToken = aac_applicationToken;
	}
	/**
	 * @return the aac_am_tenant
	 */
	public String getAac_am_tenant() {
		return aac_am_tenant;
	}
	/**
	 * @param aac_am_tenant the aac_am_tenant to set
	 */
	public void setAac_am_tenant(String aac_am_tenant) {
		this.aac_am_tenant = aac_am_tenant;
	}
}
