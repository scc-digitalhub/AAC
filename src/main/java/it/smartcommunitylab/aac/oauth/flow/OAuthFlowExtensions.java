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

package it.smartcommunitylab.aac.oauth.flow;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.AuthorizationRequest;

/**
 * OAuth 2.0 Flow extensions manager. Handles specific Flow phases, in particular
 * AfterApproval - immediately after the user has approved the requested scopes and before the code/token is emitted.
 *  
 * @author raman
 *
 */
public interface OAuthFlowExtensions {

	/**
	 * Event triggered immediately after the user has approved the requested scopes and before the code/token is emitted.
	 * @param authorizadtionRequest
	 * @param userAuthentication
	 * @throws FlowExecutionException
	 */
	public void onAfterApproval(AuthorizationRequest authorizadtionRequest, Authentication userAuthentication) throws FlowExecutionException;
	
}
