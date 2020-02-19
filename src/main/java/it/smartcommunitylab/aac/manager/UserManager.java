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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.approval.Approval.ApprovalStatus;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.AUTHORITY;
import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.dto.ConnectedAppProfile;
import it.smartcommunitylab.aac.dto.UserProfile;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.OAuthApproval;
import it.smartcommunitylab.aac.model.Registration;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.ServiceScope;
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
    private final Logger logger = LoggerFactory.getLogger(getClass());

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
	@Autowired
	private ServiceManager serviceManager;

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
	
	//TODO move to utils
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

	//TODO disable
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
	
	/**
	 * Get user with the specified username
	 * @param username
	 * @return
	 */
	public User getUserByUsername(String username) {
		return userRepository.findByUsername(username);
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
	
	
    public String getUserFullName(long userId) {
        User user = userRepository.getOne(userId);
        return user.getFullName();
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
		if (providerRoles.isEmpty()) {
		    domain = "carbon.super";
		} else {
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
	
	/**
	 * @param userId
	 * @return
	 */
	public User findOne(Long userId) {
		return userRepository.findOne(userId);
	}
	
    /**
     * @param userId
     * @return
     */
    public User getOne(Long userId) {
        User user = userRepository.findOne(userId);
        if (user == null) {
            throw new EntityNotFoundException("No user found: " + userId);
        }

        return user;
    }
    
	/**
	 * Take the currently authenticated user, the User from OAuth token, or Client owner for app token
	 * @return
	 */
	public User getUserOrOwner() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof AnonymousAuthenticationToken) {
        	throw new SecurityException("Unauthorized access");
        }
        if (auth instanceof OAuth2Authentication) {
        	OAuth2Authentication oauth = (OAuth2Authentication) auth;
        	if (oauth.isClientOnly()) {
        		String clientId = (String) oauth.getPrincipal();
        		Long userId = clientDetailsRepository.findByClientId(clientId).getDeveloperId();
        		return findOne(userId);
        	} else {
        		Object userAuth = oauth.getPrincipal();
        		String userIdString = (userAuth instanceof UserDetails) 
        				? ((UserDetails) userAuth).getUsername()
        				: (userAuth instanceof Authentication) 
        				? ((Authentication)userAuth).getPrincipal().toString()
        				: userAuth.toString();
        		return  findOne(Long.parseLong(userIdString));
        	}
        } else {
			String userIdString = auth.getName();
    		return findOne(Long.parseLong(userIdString));
        }
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

	/**
	 * @param user
	 * @return
	 */
	public List<ConnectedAppProfile> getConnectedApps(Long user) {
		List<OAuthApproval> approvals = approvalRepository.findByUserId(user.toString());
		ImmutableListMultimap<String, OAuthApproval> multimap = Multimaps.index(approvals, OAuthApproval::getClientId);
		return multimap.keySet().stream().map(e -> {
			ConnectedAppProfile p = new ConnectedAppProfile();
			p.setClientId(e);
			ClientDetailsEntity client = clientDetailsRepository.findByClientId(e);
			if (client == null) return null;
			
			p.setAppName((String)client.getAdditionalInformation().get("displayName"));
			if (p.getAppName() == null) p.setAppName(client.getName());
			p.setScopes(multimap.get(e).stream().filter(a -> a.getStatus().equals(ApprovalStatus.APPROVED.toString())).map(approval -> {
				String scope = approval.getScope();
				return serviceManager.getServiceScopeDTO(scope);
			}).collect(Collectors.toList()));
			return p;
		}).filter(p -> p != null).collect(Collectors.toList());
	}

	/**
	 * @param user
	 * @param clientId
	 * @return
	 */
	public List<ConnectedAppProfile> deleteConnectedApp(Long user, String clientId) {
		approvalRepository.delete(approvalRepository.findByUserIdAndClientId(user.toString(), clientId));
		Collection<OAuth2AccessToken> tokens = tokenStore.findTokensByClientIdAndUserName(clientId, user.toString());
		for (OAuth2AccessToken token : tokens) {
		    if(token.getRefreshToken() != null) {
		        //remove refresh token
		        OAuth2RefreshToken refreshToken = token.getRefreshToken();
		        tokenStore.removeRefreshToken(refreshToken);
		    }
		    
		    //remove access token
			tokenStore.removeAccessToken(token);
		}
		
		return getConnectedApps(user);
	}
	
	public Set<String> userScopes(User user, Set<String> scopes, boolean isUser) {
		Set<String> newScopes = Sets.newHashSet();
		Set<String> roleNames = user.getRoles().stream().map(x -> x.getAuthority()).collect(Collectors.toSet());
		logger.trace("user roles "+roleNames.toString());
		
	      // handle default case
        if (Collections.singleton("default").equals(scopes)) {
            return scopes;
        }
        
		for (String scope : scopes) {		    
			ServiceScope resource = serviceManager.getServiceScope(scope);
			logger.trace("resource for scope "+scope + " is "+String.valueOf(resource));
			if (resource != null) {
				boolean isResourceUser = resource.getAuthority().equals(AUTHORITY.ROLE_USER);
				boolean isResourceClient = !resource.getAuthority().equals(AUTHORITY.ROLE_USER);
				if (isUser && !isResourceUser) {
					continue;
				}
				if (!isUser && !isResourceClient) {
					continue;
				}
				
				if (resource.getRoles() != null && !resource.getRoles().isEmpty()) {
					Set<String> roles = Sets.newHashSet(Splitter.on(",").split(resource.getRoles()));
					if (!Sets.intersection(roleNames, roles).isEmpty()) {
						newScopes.add(scope);
					}
				} else {
					newScopes.add(scope);
				}
			}
		}
		return newScopes;
	}
}
