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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

/**
 * @author raman
 *
 */
public class ClientCredentialsFilter extends AbstractAuthenticationProcessingFilter {

	@Autowired
	private ClientDetailsRepository clientDetailsRepository = null;

	public ClientCredentialsFilter(String defaultFilterProcessesUrl) {
		super(defaultFilterProcessesUrl);
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		setAuthenticationSuccessHandler(new AuthenticationSuccessHandler() {
			public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
					Authentication authentication) throws IOException, ServletException {
				// no-op - just allow filter chain to continue to token endpoint
			}
		});
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain, Authentication authResult) throws IOException, ServletException {
		super.successfulAuthentication(request, response, chain, authResult);
		chain.doFilter(request, response);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException,
			IOException, ServletException {
		String clientId = request.getParameter("client_id");
		String clientSecret = request.getParameter("client_secret");

		// If the request is already authenticated we can assume that this filter is not needed
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()) {
			return authentication;
		}
		
		if (clientId == null) {
			throw new BadCredentialsException("No client credentials presented");
		}

		if (clientSecret == null) {
			clientSecret = "";
		}

		clientId = clientId.trim();

//		UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(clientId, clientSecret);
		ClientDetailsEntity clientDetails = clientDetailsRepository.findByClientId(clientId);
		boolean isTrusted = false;
		if (clientDetails.getAuthorities() != null) {
			for (GrantedAuthority ga : clientDetails.getAuthorities())
				if (Config.AUTHORITY.ROLE_CLIENT_TRUSTED.toString().equals(ga.getAuthority())) {
					isTrusted = true;
					break;
				}
		}
		if (!isTrusted) {
			throw new InvalidGrantException("Unauthorized client access by client "+ clientId);
		}
		
		String clientSecretServer = clientDetails.getClientSecret();
		ClientAppInfo info = ClientAppInfo.convert(clientDetails.getAdditionalInformation());
		String clientSecretMobile = clientDetails.getClientSecretMobile();
		if (clientSecretMobile.equals(clientSecret) && !info.isNativeAppsAccess()) {
			throw new InvalidGrantException("Native app access is not enabled");
		}
		
		if (!clientSecretServer.equals(clientSecret) && !clientSecretMobile.equals(clientSecret)) {
            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
		}
		
		
		User user = new User(clientId, clientSecret, clientDetails.getAuthorities());

        UsernamePasswordAuthenticationToken result = 
        		new UsernamePasswordAuthenticationToken(user,
        				clientSecretServer, user.getAuthorities());
//        result.setDetails(authRequest.getDetails());
        return result;
	}

	/**
	 * Whenever used, will be forced
	 */
//	@Override
//	protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
//		return true;
//	}

	
}
