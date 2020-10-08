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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.jaxbmodel.AuthorityMapping;
import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.model.Registration;

/**
 * Extract user attributes for the user identified by the request parameter 'token'. 
 * 
 * The token is checked to belong to the specified Google clientIds
 * provided as constructor argument for the bean (comma-separated list of client IDs)
 * 
 * The user attributes are extracted from the google userinfo API.
 * 
 * @author raman
 *
 */
public class InternalAuthorityHandler implements AuthorityHandler {

	@Autowired
	private RegistrationManager manager;

	@Override
	public Map<String, String> extractAttributes(HttpServletRequest request, Map<String,String> map, AuthorityMapping mapping) {
		String email = null;
		if (request != null) email = (String)request.getAttribute("email");
		//if (email == null) email = map.get("email");
		
		Registration user = manager.getUserByEmail(email);
		if (user == null) {
			throw new SecurityException("Error finding internal user " +email);
		}
		Map<String, String> result = new HashMap<String, String>();
		result.put("email", user.getEmail());
		result.put(Config.NAME_ATTR, user.getName());
		result.put(Config.SURNAME_ATTR, user.getSurname());
		
		return result;
	}
}
