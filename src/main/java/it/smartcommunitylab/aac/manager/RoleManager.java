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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.base.Splitter;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.dto.RoleModel;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

/**
 * Used to check whether the user has the administrator rights.
 * @author raman
 *
 */
@Component
@Transactional
public class RoleManager {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${admin.username}")
    private String adminUsername;   
    
	@Value("${admin.password}")
	private String adminPassword;	

	@Value("${admin.contexts}")
	private String[] defaultContexts;
	
	@Value("${admin.contextSpaces}")
	private String[] defaultContextSpaces;
	
	@Value("${admin.roles}")
	private String[] defaultRoles;
	
	@Autowired
	private RegistrationService registrationService;
	
	@Autowired
	private UserRepository userRepository;	
	
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;	

	@Autowired
	private ClientDetailsManager clientDetailsManager;	

	private User admin = null; 
	
	@PostConstruct
	public synchronized User init() throws Exception {
		
		if (admin != null) {
			return admin;
		}
		
	    logger.debug("init");
	    
		Set<Role> roles = new HashSet<>();
		Role role = Role.systemAdmin();
		roles.add(role);
		
		if (defaultContexts != null) {
		    logger.debug("ADMIN default contexts "+Arrays.toString(defaultContexts));
		    Arrays.asList(defaultContexts).forEach(ctx -> roles.add(Role.ownerOf(ctx)));
		}
		
		if (defaultContextSpaces != null) {
            logger.debug("ADMIN default contexts spaces "+Arrays.toString(defaultContextSpaces));		    
			Arrays.asList(defaultContextSpaces).forEach(ctx -> roles.add(Role.ownerOf(ctx)));
		}

		if (defaultRoles != null) {
            logger.debug("ADMIN default roles "+Arrays.toString(defaultRoles));		    
			Arrays.asList(defaultRoles).forEach(ctx -> roles.add(Role.parse(ctx)));
		}

		admin = userRepository.findByUsername(adminUsername);
		if (admin == null) {
		    logger.debug("create ADMIN user as "+adminUsername);
			admin = registrationService.registerOffline(adminUsername, adminUsername, adminUsername, adminPassword, null, false, null);
		}
		
		clientDetailsManager.createAdminClient(admin.getId());
		
		admin.getRoles().addAll(roles);
		userRepository.saveAndFlush(admin);
		
		
		return admin;
	}
	
	public User getAdminUser() throws Exception {
		if (admin == null) {
			init();
		}
		return admin;
	}
	
	public void updateRoles(User user, Set<Role> rolesToAdd, Set<Role> rolesToDelete) {
		Set<Role> roles = user.getRoles();
		roles.removeAll(rolesToDelete);
		roles.addAll(rolesToAdd);
		user.setRoles(roles);
		userRepository.saveAndFlush(user);
	}
	
	public void addRole(User user, Role role) {
//		Set<Role> roles = Sets.newHashSet(user.getRoles());
		if (user.getRoles() == null) {
			user.setRoles(new HashSet<>());
		}
		user.getRoles().add(role);
		userRepository.saveAndFlush(user);
	}
	
	public void removeRole(User user, Role role) {
		Set<Role> roles = Sets.newHashSet(user.getRoles());
		roles.remove(role);
		
		user.setRoles(roles);
		userRepository.saveAndFlush(user);
	}	
	
	public Set<Role> getRoles(User user) {
		return user.getRoles();
	}		
	
	public boolean hasRole(User user, Role role) {
		return user.getRoles().contains(role);
	}
	
	/**
	 * List all users where role matches the role value, context, and space
	 * @param role
	 * @param context
	 * @param space
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public List<User> findUsersByRole(String role, String context, String space, int page, int pageSize) {
		Pageable pageable = new PageRequest(page, pageSize);
		return userRepository.findByFullRole(role, context, space, pageable);
	}
	/**
	 * List all users where role matches the role value and context (arbitrary space)
	 * @param role
	 * @param context
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public List<User> findUsersByRole(String role, String context, int page, int pageSize) {
		Pageable pageable = new PageRequest(page, pageSize);
		return userRepository.findByRole(role, context, pageable);
	}

	/**
	 * List all users where role matches context and space (arbitrary role value)
	 * @param context
	 * @param space
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public List<User> findUsersByContext(String context, String space, int page, int pageSize) {
		Pageable pageable = new PageRequest(page, pageSize);
		return userRepository.findByRoleContext(context, space, pageable);
	}

	/**
	 * List all users where role matches context and space (arbitrary role value)
	 * @param context
	 * @param space
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public List<User> findUsersByContextAndRole(String context, String space, String role, int page, int pageSize) {
		Pageable pageable = new PageRequest(page, pageSize);
		return userRepository.findByRoleContextAndRole(context, space, role, pageable);
	}

	/**
	 * List all users where role matches context prefix and space (arbitrary role value)
	 * @param context
	 * @param space
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public List<User> findUsersByContextNested(String context, String space, String role, int page, int pageSize) {
		Pageable pageable = new PageRequest(page, pageSize);
		Role r = new Role();
		r.setContext(context);
		r.setSpace(space);
		String canonical = r.canonicalSpace();
		return userRepository.findByRoleContextAndRoleNested(canonical, role, context, space, pageable);
	}

	public List<GrantedAuthority> buildAuthorities(User user) {
		Set<Role> roles = getRoles(user);
		roles.add(Role.systemUser());
		List<GrantedAuthority> list = roles.stream().collect(Collectors.toList());
		return list;
	}
	
	/**
	 * Update user roles at the specified context/space according to the specified model add/delete.
	 * @param roleModel
	 * @param context
	 * @param space
	 * @return
	 */
	public List<String> updateLocalRoles(RoleModel roleModel, String context, String space) {
		String info[] = Utils.extractInfoFromTenant(roleModel.getUser());
		
		final String name = info[0];
		
		User user = userRepository.findByUsername(name);
		if (user == null) throw new EntityNotFoundException("User "+name + " does not exist");
		
		Set<Role> userRoles = new HashSet<>(user.getRoles());

		if (roleModel.getRemoveRoles() != null) {
			for (String role : roleModel.getRemoveRoles()) {
				Role r = new Role(context, space, role);
				userRoles.remove(r);
			}
		}
		if (roleModel.getAddRoles() != null) {
			for (String role : roleModel.getAddRoles()) {
				Role r = new Role(context, space, role);
				userRoles.add(r);
			}
		}
		user.getRoles().clear();
		user.getRoles().addAll(userRoles);

		userRepository.save(user);
		
		return user.spaceRole(context, space).stream().map(r -> r.getRole()).collect(Collectors.toList());
	}	

	/**
	 * Update sub-space owners for the specified context/space according to the specified model add/delete.
	 * @param roleModel
	 * @param context
	 * @param space
	 * @return
	 */
	public List<String> updateLocalOwners(RoleModel roleModel, String context, String space) {
		String info[] = Utils.extractInfoFromTenant(roleModel.getUser());
		
		final String name = info[0];
		
		Role parent = new Role(context, space, Config.R_PROVIDER);
		String parentContext = parent.canonicalSpace();
		
		User user = userRepository.findByUsername(name);
		if (user == null) throw new EntityNotFoundException("User "+name + " does not exist");
		
		Set<Role> userRoles = new HashSet<>(user.getRoles());

		if (roleModel.getRemoveRoles() != null) {
			for (String child : roleModel.getRemoveRoles()) {
				Role r = new Role(parentContext, child, Config.R_PROVIDER);
				userRoles.remove(r);
			}
		}
		if (roleModel.getAddRoles() != null) {
			for (String child : roleModel.getAddRoles()) {
				Role r = new Role(parentContext, child, Config.R_PROVIDER);
				userRoles.add(r);
			}
		}
		user.getRoles().clear();
		user.getRoles().addAll(userRoles);

		userRepository.save(user);
		
		return user.contextRole(Config.R_PROVIDER, parentContext).stream().map(r -> r.getSpace()).collect(Collectors.toList());
	}	
	
	public Set<Role> getRoles(Long userId) throws Exception {
		User user = userRepository.findOne(userId);
		return user.getRoles();
	}

	public void addRoles(Long userId, String clientId, String roles, boolean asRoleManager) throws Exception {
		ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
		Long developerId = client.getDeveloperId();
		User user = userRepository.findOne(userId);

		User developer = userRepository.findOne(developerId);
		Set<Role> fullRoles = parseAndCheckRoles(roles);
		if (!asRoleManager) {
			// role should be in the same space or in the same context if it is ROLE_PROVIDER
			Set<String> acceptedDomains = developer.contextRole(Config.R_PROVIDER).stream().map(Role::canonicalSpace).collect(Collectors.toSet());
			if (fullRoles.stream()
					.anyMatch(role -> !acceptedDomains.contains(role.canonicalSpace())  && !(acceptedDomains.contains(role.getContext()) && Config.R_PROVIDER.equals(role.getRole())))) {
				throw new IllegalArgumentException("Can add roles to the owned space or create new child space owners");
			}
		}
		user.getRoles().addAll(fullRoles);
		userRepository.save(user);
	}

	protected Set<Role> parseAndCheckRoles(String roles) {
		Set<Role> fullRoles = new HashSet<>();
		List<String> input = Splitter.on(",").splitToList(roles);
		for (String roleString : input) {
			Role role = Role.parse(roleString);
			fullRoles.add(role);
		}
		if (fullRoles.isEmpty()) throw new IllegalArgumentException("Invalid input roles");
		return fullRoles;
	}

	public void deleteRoles(Long userId, String clientId, String roles, boolean asRoleManager) throws Exception {

		ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
		Long developerId = client.getDeveloperId();

		User developer = userRepository.findOne(developerId);
		Set<Role> fullRoles = parseAndCheckRoles(roles);

		if (!asRoleManager) {
			// cannot remove ROLE_PROVIDER of the same user
			Set<String> acceptedDomains = developer.contextRole(Config.R_PROVIDER).stream().map(Role::canonicalSpace).collect(Collectors.toSet());
			if (developerId == userId && fullRoles.stream()
					.anyMatch(role -> Config.R_PROVIDER.equals(role.getRole()))) {
				throw new IllegalArgumentException("Cannot remove space ownership for the same user");
			}
			// can remove roles in the same space or ROLE_PROVIDERs of subspaces
			if (fullRoles.stream()
					.anyMatch(role -> !acceptedDomains.contains(role.canonicalSpace())  && !(acceptedDomains.contains(role.getContext()) && Config.R_PROVIDER.equals(role.getRole())))) {
				throw new IllegalArgumentException("Can delete roles only within owned spaces");
			}
		}
		User user = userRepository.findOne(userId);
		user.getRoles().removeAll(fullRoles);
		userRepository.save(user);
	}

	public Multimap<String, String> getRoleSpacesToNarrow(String clientId, Collection<? extends GrantedAuthority> authorities) {
		try {
			ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
			ClientAppInfo info = ClientAppInfo.convert(client.getAdditionalInformation());
			if (info.getUniqueSpaces() != null && !info.getUniqueSpaces().isEmpty()) {
				Multimap<String, String> map = LinkedListMultimap.create();
				for (GrantedAuthority a : authorities) {
					Role r = Role.parse(a.getAuthority());
					if (!StringUtils.isEmpty(r.getContext()) && info.getUniqueSpaces().contains(r.getContext())) {
						map.put(r.getContext(), r.getSpace());
					} 
				}
				//remove keys if single element in list, nothing to choose
				map.keySet().removeIf(k -> map.get(k).size() == 1);

				if (map.isEmpty()) return null;
				return map;
			}
			return null;
		}
		catch (ClientRegistrationException e) {
			return null;
		}		
		
	}


	/**
	 * @param map
	 * @param authorities
	 * @return
	 */
	public Collection<? extends GrantedAuthority> narrowRoleSpaces(Map<String, String> map, Collection<? extends GrantedAuthority> authorities) {
		return authorities.stream().map(a -> Role.parse(a.getAuthority())).filter(r -> !map.containsKey(r.getContext()) || map.getOrDefault(r.getContext(), "").equals(r.getSpace())).collect(Collectors.toSet());
	}

}
