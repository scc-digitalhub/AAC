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
package it.smartcommunitylab.aac.oauth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.provider.ClientDetails;

import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

/**
 * Implementation of the {@link UserDetailsService} based on the Client Details model.
 * @author raman
 *
 */
public class OAuthClientUserDetails implements UserDetailsService {

	private ClientDetailsRepository clientDetailsRepository;

	public OAuthClientUserDetails(ClientDetailsRepository clientDetailsRepository) {
		super();
		this.clientDetailsRepository = clientDetailsRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
		ClientDetails details = clientDetailsRepository.findByClientId(userName);
		if (details == null) throw new BadCredentialsException("Client does not exist");
		User user = new User(details.getClientId(), details.getClientSecret(), details.getAuthorities());
		return user;
	}

}
