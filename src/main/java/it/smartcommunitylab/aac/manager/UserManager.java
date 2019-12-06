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

package it.smartcommunitylab.aac.manager;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.PasswordHash;
import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.dto.UserProfile;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Registration;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.AACAuthenticationToken;
import it.smartcommunitylab.aac.oauth.AACOAuthRequest;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.OAuthApprovalRepository;
import it.smartcommunitylab.aac.repository.RegistrationRepository;
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
	@Autowired
	private OAuthApprovalRepository approvalRepository;
	@Autowired
	private JdbcTokenStore tokenStore;
	@Autowired
	private RegistrationRepository regRepository;
	@Autowired
	private RegistrationManager regManager;
	
	@Value("${api.contextSpace}")
	private String apiProviderContext;

	/**
	 * Check that the specified client is owned by the currently logged user
	 * @param clientId
	 * @throws AccessDeniedException 
	 */
	public void checkClientIdOwnership(String clientId) throws AccessDeniedException {
		ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
		if (client == null || !client.getDeveloperId().equals(getUserId())) {
			throw new SecurityException("Attempt modifyung non-owned client app data");
		};
	}
	
	/**
	 * Get the user from the Spring Security Context
	 * @return
	 * @throws AccessDeniedException 
	 */
	private UserDetails getUserDetails() throws AccessDeniedException{
		try {
			return (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		} catch (Exception e) {
			throw new AccessDeniedException("Incorrect user");
		}
	}
	
	/**
	 * @return the user ID (long) from the user object in Spring Security Context
	 */
	public Long getUserId() throws AccessDeniedException {
		try {
			return Long.parseLong(getUserDetails().getUsername());
		} catch (Exception e) {
			throw new AccessDeniedException("Incorrect username format");
		}
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
	 * @throws AccessDeniedException 
	 */
	public String getUserFullName() throws AccessDeniedException {
		User user =  userRepository.findOne(getUserId());
		return user.getFullName();
	}

	/**
	 * Get user DB object
	 * @return {@link User} object
	 * @throws AccessDeniedException 
	 */
	public User getUser() throws AccessDeniedException {
		User user = userRepository.findOne(getUserId());
		return user;
	}
	
	public Set<Role> getOwnedSpaceAt(String context) throws AccessDeniedException {
		User user = getUser();
		if (user == null) {
			return null;
		}
		Set<Role> providerRoles = user.contextRole(Config.R_PROVIDER, context);
		if (providerRoles.isEmpty()) return Collections.emptySet();
		return providerRoles;
	}

	/**
	 * Currently constructs name as the `email @ apimanager-tenant`   
	 * @param l
	 * @return
	 */
	public String getUserInternalName(long userId) {
		User user = userRepository.findOne(userId);
		if (user == null) throw new EntityNotFoundException("No user found: "+userId);
		Set<Role> providerRoles = user.contextRole(Config.R_PROVIDER, apiProviderContext);

		String domain = null;
		if (providerRoles.isEmpty()) domain = "carbon.super";
		else {
			Role role = providerRoles.iterator().next();
			domain = role.getSpace();
		}
		
		return Utils.getUserNameAtTenant(user.getUsername(), domain);
	}	
	
	/**
	 * Get all the roles of the user for the specified clientId
	 * @param user
	 * @param clientId
	 * @return
	 */
	public Set<Role> getUserRolesByClient(User user, String clientId, boolean asRoleManager) {
		if (asRoleManager) return user.getRoles();
		
		ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
		Long developerId = client.getDeveloperId();

		User developer = userRepository.findOne(developerId);
		Set<String> providerSpaces = developer.contextRole(Config.R_PROVIDER).stream().map(Role::canonicalSpace).collect(Collectors.toSet());
		if (providerSpaces.isEmpty()) {
			return Collections.emptySet();
		}

		Set<Role> roles = user.getRoles().stream().filter(x -> providerSpaces.contains(x.canonicalSpace()))
				.collect(Collectors.toSet());

		return roles;
	}
	
	/**
	 * Get all the roles of the user for the specified clientId
	 * @param user
	 * @param clientId
	 * @return
	 */
	public Set<Role> getUserRolesByClient(Long userId, String clientId, boolean asRoleManager) {
		User user = userRepository.findOne(userId);
		if (user == null) throw new EntityNotFoundException("No user found: "+userId);
		return getUserRolesByClient(user, clientId, asRoleManager);
	}

	/**
	 * @param developerId
	 * @return
	 * @throws AccessDeniedException 
	 */
	public Set<Role> getUserRoles(Long developerId) throws AccessDeniedException {
		User user = userRepository.findOne(developerId);
		if (user != null) {
			return user.getRoles();
		}
		return Collections.emptySet();
	}
	
	public String getUserAsClient() {
		return (String)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}

	/**
	 * @param userId
	 * @return
	 */
	public User findOne(Long userId) {
		return userRepository.findOne(userId);
	}

	/**
	 * @param user
	 */
	public void deleteUser(Long userId) {
		// TODO revoke social?
		User user = userRepository.findOne(userId);
		clientDetailsRepository.delete(clientDetailsRepository.findByDeveloperId(userId));
		
		approvalRepository.delete(approvalRepository.findByUserId(userId.toString()));

		Collection<OAuth2AccessToken> tokens = tokenStore.findTokensByUserName(userId.toString());
		for (OAuth2AccessToken token : tokens) {
			tokenStore.removeAccessToken(token);
		}
		Registration reg = regRepository.findByEmail(user.getUsername());
		if (reg != null) {
			regRepository.delete(reg);
		}
		userRepository.delete(userId);
	}

	/**
	 * @param user
	 * @param profile
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchAlgorithmException 
	 */
	public void updateProfile(Long userId, UserProfile profile) throws Exception {
		User user = userRepository.findOne(userId);
		profile.setUsername(user.getUsername());
		Registration reg = regRepository.findByEmail(profile.getUsername());
		if (reg == null) {
			regManager.registerOffline(profile.getName(), profile.getSurname(), profile.getUsername(), profile.getPassword(), profile.getLang(), false, null);
		} else {
			regManager.updateRegistration(profile.getUsername(), profile.getName(), profile.getSurname(), profile.getPassword(), profile.getLang());
		}
	}
}
