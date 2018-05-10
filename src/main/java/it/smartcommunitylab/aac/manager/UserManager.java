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

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.smartcommunitylab.aac.Config.ROLE_SCOPE;
import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.AACAuthenticationToken;
import it.smartcommunitylab.aac.oauth.AACOAuthRequest;
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

	public static final String R_PROVIDER = "ROLE_PROVIDER";
	public static final String R_ROLEMANAGER = "rolemanager";
	
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
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof AACAuthenticationToken) {
			AACAuthenticationToken aacToken = (AACAuthenticationToken)authentication;
			AACOAuthRequest request = (AACOAuthRequest) aacToken.getDetails();
			return request.getAuthority();
		}
		return null;
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
		return user.getFullName();
	}

	/**
	 * Get user DB object
	 * @return {@link User} object
	 */
	public User getUser() {
		User user = userRepository.findOne(getUserId());
		return user;
	}
	
	public String getProviderDomain() {
		User user = getUser();
		if (user == null) {
			return null;
		}
		Set<Role> providerRoles = user.role(ROLE_SCOPE.tenant, R_PROVIDER);
		if (providerRoles.isEmpty()) return null;
		
		
		Role role = providerRoles.iterator().next();
		
		return role.getContext();
	}

	/**
	 * @param l
	 * @return
	 */
	public String getUserInternalName(long userId) {
		User user = userRepository.findOne(userId);
		if (user == null) throw new EntityNotFoundException("No user found: "+userId);
		Set<Role> providerRoles = user.role(ROLE_SCOPE.tenant, "ROLE_PROVIDER");

		String domain = null;
		if (providerRoles.isEmpty()) domain = "carbon.super";
		else {
			Role role = providerRoles.iterator().next();
			domain = role.getContext();
		}
		
		return Utils.getUserNameAtTenant(user.getUsername(), domain);
	}	
	
	/**
	 * Get all the roles of the user for the specified clientId
	 * @param user
	 * @param clientId
	 * @return
	 */
	public Set<Role> getUserRolesByClient(User user, String clientId) {
		ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
		Long developerId = client.getDeveloperId();

		User developer = userRepository.findOne(developerId);
		Optional<Role> provider = developer.getRoles().stream().filter(x -> "ROLE_PROVIDER".equals(x.getRole())).findFirst();
		if (!provider.isPresent()) {
			return Collections.emptySet();
		}
		String tenant = provider.get().getContext();

		Set<Role> roles = user.getRoles().stream().filter(x -> tenant.equals(x.getContext()))
				.collect(Collectors.toSet());

		return roles;
	}
	
	/**
	 * Get all the roles of the user for the specified clientId
	 * @param user
	 * @param clientId
	 * @return
	 */
	public Set<Role> getUserRolesByClient(Long userId, String clientId) {
		User user = userRepository.findOne(userId);
		if (user == null) throw new EntityNotFoundException("No user found: "+userId);
		return getUserRolesByClient(user, clientId);
	}

	/**
	 * @param developerId
	 * @return
	 */
	public Set<Role> getUserRoles(Long developerId) {
		User user = userRepository.findOne(getUserId());
		if (user != null) {
			return user.getRoles();
		}
		return Collections.emptySet();
	}
	
}
