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

package it.smartcommunitylab.aac.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

/**
 * Base class for the app controllers. 
 * @author raman
 *
 */
public class AbstractController {

	@Autowired
	private ClientDetailsRepository clientDetailsRepository;

	/**
	 * Check that the specified client is owned by the currently logged user
	 * @param clientId
	 */
	protected void checkClientIdOwnership(String clientId) {
		ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
		if (client == null || !client.getDeveloperId().equals(getUserId())) {
			throw new SecurityException("Attempt modifyung non-owned client app data");
		};
	}
	
	/**
	 * Get the user from the Spring Security Context
	 * @return
	 */
	protected UserDetails getUser(){
		return (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}
	
	/**
	 * @return the user ID (long) from the user object in Spring Security Context
	 */
	protected Long getUserId() {
		return Long.parseLong(getUser().getUsername());
	}

	/**
	 * The authority (e.g., google) value from the Spring Security Context of the currently logged user
	 * @return the authority value (string)
	 */
	protected String getUserAuthority() {
		return SecurityContextHolder.getContext().getAuthentication().getDetails().toString();
	}

	/**
	 * Read the signed user name from the user attributes
	 * @param user
	 * @return
	 */
	protected String getUserName(User user) {
		return user.getName() + " "+user.getSurname();
	}

}
