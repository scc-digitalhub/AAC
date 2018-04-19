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
package it.smartcommunitylab.aac.oauth;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import it.smartcommunitylab.aac.repository.UserRepository;

/**
 * Implementation of the {@link UserDetailsService} based on the SC user model.
 * @author raman
 *
 */
public class UserDetailsRepo implements UserDetailsService {

	private UserRepository userRepository;

	public UserDetailsRepo(UserRepository userRepository) {
		super();
		this.userRepository = userRepository;
	}



	@Override
	public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
		List<GrantedAuthority> list = Collections.<GrantedAuthority>singletonList(new SimpleGrantedAuthority("ROLE_USER"));
		
		Long id = null;
		// expected that the user name is the numerical identifier
		try {
			id = Long.parseLong(userName);
		} catch (NumberFormatException e) {
			throw new UsernameNotFoundException("Incorrect user ID: "+ userName);
		}
		
		it.smartcommunitylab.aac.model.User userEntity = userRepository.findOne(id);
		if (userEntity == null) throw new UsernameNotFoundException("User with id "+id +" does not exist.");
		return new User(userEntity.getId().toString(),"", list);
	}

}
