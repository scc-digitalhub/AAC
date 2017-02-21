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

package it.smartcommunitylab.aac.test;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import it.smartcommunitylab.aac.authority.AuthorityHandler;
import it.smartcommunitylab.aac.jaxbmodel.AuthorityMapping;

/**
 * @author raman
 *
 */
public class GoogleAuthorityHandler implements AuthorityHandler{

	@Override
	public Map<String, String> extractAttributes(HttpServletRequest request, Map<String, String> map, AuthorityMapping mapping) {
		Map<String, String> result = new HashMap<String, String>();
		
		result.put("id", "12345");
		result.put("given_name", "mario");
		result.put("family_name", "rossi");
		result.put("email", "mario.rossi@gmail.com");
		
		return result;
	}

}
