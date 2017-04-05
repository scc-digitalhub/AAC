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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.ROLE_SCOPE;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
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

	@Autowired
	@Value("${security.adminfile}")
	private Resource adminFile;
	
	@Value("${admin.password}")
	private String adminPassword;	

	@Autowired
	private RegistrationManager registrationManager;
	
	@Autowired
	private UserRepository userRepository;	
	
	
	
	@PostConstruct
	public void init() {
		try {
			User admin = registrationManager.registerOffline("admin", "admin", "admin", adminPassword, null, false, null);
			Role role = new Role(ROLE_SCOPE.system, Config.R_ADMIN, null);
			admin.setRoles(Sets.newHashSet(role));
			userRepository.saveAndFlush(admin);
		} catch (AlreadyRegisteredException e1) {
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
	
	public List<GrantedAuthority> buildAuthorities(User user) {
		Set<Role> roles = getRoles(user);
		
		List<GrantedAuthority> list = roles.stream().collect(Collectors.toList());

		return list;
	}
	
	
//	public List<GrantedAuthority> buildAuthorities(User user, String provider) {
//		List<GrantedAuthority> list = new LinkedList<>();
//		list.add(new SimpleGrantedAuthority(ROLE.user.roleName()));
//
//		Set<Identity> identityAttrs = new HashSet<Identity>();
//		for (Attribute a : user.getAttributeEntities()) {
//			if (a.getAuthority().getName().equals(provider) && 
//				attributesAdapter.isIdentityAttr(a)) {
//				identityAttrs.add(new Identity(provider, a.getKey(), a.getValue(), null));
//			}
//		}
//		try {
//			for (Identity test : readIdentities()) {
//				if (identityAttrs.contains(test)) {
//					list.add(new SimpleGrantedAuthority(ROLE.valueOf(test.getRole()).roleName()));
//				}
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return list;
//	}
//	
//	public boolean checkAccount(Set<Identity> identityStrings, ROLE role) throws Exception {
//		for (Identity test : readIdentities()) {
//			if (role.name().equals(test.getRole()) && identityStrings.contains(test)) return true;
//		}
//		return false;
//	}
//	
//	private List<Identity> readIdentities() throws IOException {
//		List<Identity> res = new LinkedList<>();
//		BufferedReader reader = new BufferedReader(new InputStreamReader(adminFile.getInputStream()));
//		String line = null;
//		while ((line = reader.readLine()) != null) {
//			if (line.startsWith("#")) continue;
//
//			String[] arr = line.split(";");
//			if (arr.length != 4) continue;
//				
//			Identity test = new Identity(arr[0].trim(), arr[1].trim(), arr[2].trim(), arr[3].trim().toLowerCase());
//			res.add(test);
//		}
//		return res;
//	}
}
