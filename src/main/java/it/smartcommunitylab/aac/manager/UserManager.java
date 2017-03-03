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

package it.smartcommunitylab.aac.manager;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.Attribute;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

/**
 * Logged in user data manager. 
 * @author raman
 *
 */
@Component
@Transactional
public class UserManager {

	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	@Autowired
	private UserRepository userRepository;

	/**
	 * Check that the specified client is owned by the currently logged user
	 * @param clientId
	 */
	public void checkClientIdOwnership(String clientId) {
		ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
		if (client == null || !client.getDeveloperId().equals(getUserId())) {
			throw new SecurityException("Attempt modifyung non-owned client app data");
		};
	}
	
	/**
	 * Get the user from the Spring Security Context
	 * @return
	 */
	private UserDetails getUserDetails(){
		return (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}
	
	/**
	 * @return the user ID (long) from the user object in Spring Security Context
	 */
	public Long getUserId() {
		return Long.parseLong(getUserDetails().getUsername());
	}

	/**
	 * The authority (e.g., google) value from the Spring Security Context of the currently logged user
	 * @return the authority value (string)
	 */
	public String getUserAuthority() {
		return SecurityContextHolder.getContext().getAuthentication().getDetails().toString();
	}

	/**
	 * The authority (e.g., google) value from the Spring Security Context of the currently logged user
	 * @return the authority value (string)
	 */
	public Set<String> getUserRoles() {
		Set<String> res = new HashSet<>();
		SecurityContextHolder.getContext().getAuthentication().getAuthorities().forEach(ga -> res.add(ga.getAuthority()));
		return res;
	}

	/**
	 * Read the signed user name from the user attributes
	 * @param user
	 * @return
	 */
	public String getUserFullName() {
		User user =  userRepository.findOne(getUserId());
		return user.getName() + " "+user.getSurname();
	}

	/**
	 * Get user DB object
	 * @return {@link User} object
	 */
	public User getUser() {
		User user = userRepository.findOne(getUserId());
		return user;
	}

}
