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

import java.util.List;

import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceScopeDTO;

/**
 * @author raman
 *
 */
public class ApprovalData {

	private String name;
	private String clientId;
	private List<ServiceScopeDTO> scopes;
	private String owner;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public List<ServiceScopeDTO> getScopes() {
		return scopes;
	}
	public void setScopes(List<ServiceScopeDTO> scopes) {
		this.scopes = scopes;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	
	
}
