/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
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
 ******************************************************************************/

package it.smartcommunitylab.aac.manager;

import javax.transaction.Transactional;

import org.springframework.security.oauth2.provider.ClientDetails;

import it.smartcommunitylab.aac.oauth.OAuth2ClientDetailsProvider;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

/**
 * @author raman
 *
 */
@Transactional
public class OAuth2ClientDetailsProviderImpl implements OAuth2ClientDetailsProvider {

	private ClientDetailsRepository repo;
	
	/**
	 * @param clientDetailsRepository
	 */
	public OAuth2ClientDetailsProviderImpl(ClientDetailsRepository clientDetailsRepository) {
		this.repo = clientDetailsRepository;
	}

	@Override
	public ClientDetails getClientDetails(String clientId) {
		return repo.findByClientId(clientId);
	}

}
