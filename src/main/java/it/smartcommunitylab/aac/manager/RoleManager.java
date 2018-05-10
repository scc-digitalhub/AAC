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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.ROLE_SCOPE;
import it.smartcommunitylab.aac.apimanager.model.DataList;
import it.smartcommunitylab.aac.apimanager.model.RoleModel;
import it.smartcommunitylab.aac.apimanager.model.Subscription;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.UserRepository;

/**
 * Used to check whether the user has the administrator rights.
 * @author raman
 *
 */
@Component
public class RoleManager {

	@Value("${admin.password}")
	private String adminPassword;	

	@Autowired
	private RegistrationService registrationService;
	
	@Autowired
	private UserRepository userRepository;	
	
	
	public User init() {
		try {
			User admin = registrationService.registerOffline("admin", "admin", "admin", adminPassword, null, false, null);
			Role role = new Role(ROLE_SCOPE.system, Config.R_ADMIN, null);
			Role providerRole = new Role(ROLE_SCOPE.tenant, UserManager.R_PROVIDER, "carbon.super");
			
			admin.setRoles(Sets.newHashSet(role, providerRole));
			userRepository.saveAndFlush(admin);
			return admin;
		} catch (AlreadyRegisteredException e1) {
			return userRepository.findByName("admin");
		}
	}
	
	
	public void updateRoles(User user, Set<Role> roles) {
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
	
	public List<User> findUsersByRole(ROLE_SCOPE scope, String role, int page, int pageSize) {
		Pageable pageable = new PageRequest(page, pageSize);
		return userRepository.findByPartialRole(role, scope, pageable);
	}
	public List<User> findUsersByRole(ROLE_SCOPE scope, String role, String context, int page, int pageSize) {
		Pageable pageable = new PageRequest(page, pageSize);
		return userRepository.findByFullRole(role, scope, context, pageable);
	}

	public List<User> findUsersByContext(ROLE_SCOPE scope, String context, int page, int pageSize) {
		Pageable pageable = new PageRequest(page, pageSize);
		return userRepository.findByRoleContext(scope, context, pageable);
	}

	
	public List<GrantedAuthority> buildAuthorities(User user) {
		Set<Role> roles = getRoles(user);
		
		List<GrantedAuthority> list = roles.stream().collect(Collectors.toList());

		return list;
	}
	
	
	public void fillRoles(DataList<Subscription> subs, String domain) {
		for (Subscription sub: subs.getList()) {
			String subscriber = sub.getSubscriber();
			String info[] = Utils.extractInfoFromTenant(subscriber);
			final String name = info[0];
			
			User user = userRepository.findByUsername(name);
			
			Set<Role> userRoles = user.getRoles();
			List<String> roleNames = userRoles.stream().filter(x -> domain.equals(x.getContext())).map(r -> r.getRole()).collect(Collectors.toList());
			sub.setRoles(roleNames);
		}
	}	
	
	public List<String> updateLocalRoles(RoleModel roleModel, String domain) {
		String info[] = Utils.extractInfoFromTenant(roleModel.getUser());
		
		final String name = info[0];
		
		User user = userRepository.findByUsername(name);

		Set<Role> userRoles = new HashSet<Role>(user.getRoles());

		if (roleModel.getRemoveRoles() != null) {
			for (String role : roleModel.getRemoveRoles()) {
				Role r = new Role(ROLE_SCOPE.application, role, domain);
				userRoles.remove(r);
			}
		}
		if (roleModel.getAddRoles() != null) {
			for (String role : roleModel.getAddRoles()) {
				Role r = new Role(ROLE_SCOPE.application, role, domain);
				userRoles.add(r);
			}
		}
		user.getRoles().clear();
		user.getRoles().addAll(userRoles);

		userRepository.save(user);
		
		return userRoles.stream().filter(x -> domain.equals(x.getContext()) && ROLE_SCOPE.application.equals(x.getScope())).map(r -> r.getRole()).collect(Collectors.toList());
	}	

}
