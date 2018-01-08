/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
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

package it.smartcommunitylab.aac.authority;

import java.util.Map;

import it.smartcommunitylab.aac.jaxbmodel.AuthorityMapping;

/**
 * An interface to handle native login Identity Provider authentication with IdP-specific tokens
 * @author raman
 *
 */
public interface NativeAuthorityHandler {

	/**
	 * Extract the authentication attributes of the Identity Provider authority
	 * @param token 
	 * @param map with custom parameters of the original request
	 * @param mapping {@link AuthorityMapping} descriptor for the authority
	 * @return user attributes
	 */
	Map<String,String> extractAttributes(String token, Map<String, String> map, AuthorityMapping mapping) throws SecurityException ;

	/**
	 * Extract username from the identified authority parameters
	 * @param map
	 * @return
	 */
	String extractUsername(Map<String, String> map);

}
