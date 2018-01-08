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

import java.util.HashSet;
import java.util.Set;

import it.smartcommunitylab.aac.Config.ROLE_SCOPE;
import it.smartcommunitylab.aac.model.User;

/**
 * @author raman
 *
 */
public class UserDTO {

	private String userId, fullname, username;

	private Set<String> roles;
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getFullname() {
		return fullname;
	}
	public void setFullname(String fullname) {
		this.fullname = fullname;
	}
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public Set<String> getRoles() {
		return roles;
	}
	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}
	public static UserDTO fromUser(User user, String context, ROLE_SCOPE scope) {
		UserDTO res = new UserDTO();
		res.setFullname(user.getFullName());
		res.setUserId(user.getId().toString());
		res.setUsername(user.getUsername());
		res.setRoles(new HashSet<String>());
		user.getRoles().forEach(r -> {
			if ((context == null && r.getContext() == null || context != null && context.equals(r.getContext())) &&
				(scope == null || scope.equals(r.getScope())))  res.getRoles().add(r.getRole());
		});
		return res;
	}
}
