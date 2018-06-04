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

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import it.smartcommunitylab.aac.Config;

/**
 * @author raman
 *
 */
public class AACRememberMeServices extends PersistentTokenBasedRememberMeServices {

	/**
	 * @param key
	 * @param userDetailsService
	 * @param tokenRepository
	 */
	public AACRememberMeServices(String key, UserDetailsService userDetailsService, PersistentTokenRepository tokenRepository) {
		super(key, userDetailsService, tokenRepository);
	}

	@Override
	protected boolean rememberMeRequested(HttpServletRequest request, String parameter) {
		if (super.rememberMeRequested(request, parameter)) { 
			return true;
		}
		
		AACOAuthRequest oauthRequest = (AACOAuthRequest) request.getSession().getAttribute(Config.SESSION_ATTR_AAC_OAUTH_REQUEST);
		if (oauthRequest != null && oauthRequest.isRememberMe()) {
			return true;
		}
		return false;
	}
	
	

}
