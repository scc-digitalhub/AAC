/*******************************************************************************
 * Copyright 2015-2019 Smart Community Lab, FBK
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.jaxbmodel.Attributes;
import it.smartcommunitylab.aac.jaxbmodel.AuthorityMapping;

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
public class FBNativeAuthorityHandler implements NativeAuthorityHandler {

	private RestTemplate restTemplate = new RestTemplate();

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> extractAttributes(String token, Map<String,String> map, AuthorityMapping mapping) throws SecurityException {
		try {
			// first, we have to validate that the token is a correct platform token
			Map<String, Object> s = restTemplate.getForObject("https://graph.facebook.com/v2.9/me?fields=name,first_name,last_name,picture,email&access_token="+token, Map.class);
			
			return extractAttributes(s, mapping);
		} catch (Exception e) {
			throw new SecurityException("Error validating facebook token " +token + ": " + e.getMessage());
		}
	}

	@Override
	public String extractUsername(Map<String, String> map) {
		if (map.get(Config.USER_ATTR_USERNAME) != null) return map.get(Config.USER_ATTR_USERNAME);
		return map.get("id") + "@facebook";
	}


	/**
	 * @param result
	 * @return
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	private Map<String, String> extractAttributes(Map<String, Object> user, AuthorityMapping mapping) throws JsonParseException, JsonMappingException, IOException {
		Map<String, String> attrs = new HashMap<String, String>(); 
		for (String key : mapping.getIdentifyingAttributes()) {
			Object value = user.get(key);
			if (value != null) {
				attrs.put(key, value.toString());
			}
		}
		for (Attributes attribute : mapping.getAttributes()) {
			// used alias if present to set attribute in map
			Object value = user.get(attribute.getValue());
			if (value != null) {
				String key = !StringUtils.isEmpty(attribute.getAlias()) ? attribute.getAlias() : attribute.getValue();
				attrs.put(key, value.toString());
			}
		}
		return attrs;	
	}
}
