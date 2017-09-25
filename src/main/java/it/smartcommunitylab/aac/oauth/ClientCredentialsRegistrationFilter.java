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

package it.smartcommunitylab.aac.oauth;

import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.util.OAuth2Utils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

/**
 * @author raman
 *
 */
public class ClientCredentialsRegistrationFilter extends ClientCredentialsTokenEndpointFilter {

	/**
	 * @param clientDetailsRepository
	 */
	public ClientCredentialsRegistrationFilter(ClientDetailsRepository clientDetailsRepository) {
		super(clientDetailsRepository);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {

		String clientId = request.getParameter(OAuth2Utils.CLIENT_ID);
		String clientSecret = request.getParameter("client_secret");

		if (clientId == null) {
			throw new BadCredentialsException("No client credentials presented");
		}

		if (clientSecret == null) {
			clientSecret = "";
		}

		clientId = clientId.trim();

		ClientDetailsEntity clientDetails = clientDetailsRepository.findByClientId(clientId);
		if (clientDetails == null) {
			throw new BadCredentialsException("No client found");
		}

		checkInternalIdP(clientDetails);
		
		String clientSecretServer = clientDetails.getClientSecret();
		Set<String> grantTypes = clientDetails.getAuthorizedGrantTypes();
		checkCredentialsWithMobile(clientSecret, clientDetails, grantTypes, clientSecretServer);

		if (clientDetails.getScope() == null || !clientDetails.getScope().contains(Config.SCOPE_USERMANAGEMENT)) {
			throw new BadCredentialsException("Insufficient permissions for user management");
		}
		
		return createAuthentication(clientId, clientDetails, clientSecretServer);
	}


}
