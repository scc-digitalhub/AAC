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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.model.Attribute;
import it.smartcommunitylab.aac.model.Identity;
import it.smartcommunitylab.aac.model.User;

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

	@Autowired
	private AttributesAdapter attributesAdapter;
	
	public enum ROLE {
		admin ("ROLE_ADMIN"), 
		user ("ROLE_USER"), 
		developer ("ROLE_DEVELOPER"), 
		manager ("ROLE_MANAGER");
		
		private final String roleName;

		private ROLE(String roleName) {
			this.roleName = roleName;
		}
		
		public String roleName(){
			return roleName;
		}
	};
	
	public List<GrantedAuthority> buildAuthorities(User user, String provider) {
		List<GrantedAuthority> list = new LinkedList<>();
		list.add(new SimpleGrantedAuthority(ROLE.user.roleName()));

		Set<Identity> identityAttrs = new HashSet<Identity>();
		for (Attribute a : user.getAttributeEntities()) {
			if (a.getAuthority().getName().equals(provider) && 
				attributesAdapter.isIdentityAttr(a)) {
				identityAttrs.add(new Identity(provider, a.getKey(), a.getValue(), null));
			}
		}
		try {
			for (Identity test : readIdentities()) {
				if (identityAttrs.contains(test)) {
					list.add(new SimpleGrantedAuthority(ROLE.valueOf(test.getRole()).roleName()));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}
	
	public boolean checkAccount(Set<Identity> identityStrings, ROLE role) throws Exception {
		for (Identity test : readIdentities()) {
			if (role.name().equals(test.getRole()) && identityStrings.contains(test)) return true;
		}
		return false;
	}
	
	private List<Identity> readIdentities() throws IOException {
		List<Identity> res = new LinkedList<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(adminFile.getInputStream()));
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#")) continue;

			String[] arr = line.split(";");
			if (arr.length != 4) continue;
				
			Identity test = new Identity(arr[0].trim(), arr[1].trim(), arr[2].trim(), arr[3].trim().toLowerCase());
			res.add(test);
		}
		return res;
	}
}
