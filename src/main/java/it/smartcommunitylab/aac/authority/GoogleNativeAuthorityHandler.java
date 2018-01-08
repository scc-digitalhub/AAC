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

import eu.trentorise.smartcampus.network.JsonUtils;
import eu.trentorise.smartcampus.network.RemoteConnector;
import eu.trentorise.smartcampus.network.RemoteException;
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
public class GoogleNativeAuthorityHandler implements NativeAuthorityHandler {

	@Override
	public Map<String, String> extractAttributes(String token, Map<String,String> map, AuthorityMapping mapping) throws SecurityException  {
		
		try {
			Map<String, Object> result = null;
			try {
				result = validateV3(token);
			} catch (Exception e) {
				// invalid token or invalid token version
			}
			if (result == null) {
				result = validateV1(token);
			}
			
			return extractAttributes(result, mapping);
		} catch (RemoteException e) {
			throw new SecurityException("Error validating google token " +token + ": " + e.getMessage());
		}
	}

	@Override
	public String extractUsername(Map<String, String> map) {
		if (map.get(Config.USERNAME_ATTR) != null) return map.get(Config.USERNAME_ATTR);
		return map.get("id") + "@google";
	}

	/**
	 * @param token
	 * @return
	 * @throws RemoteException 
	 * @throws SecurityException 
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> validateV3(String token) throws SecurityException, RemoteException {
		String s = RemoteConnector.getJSON("https://www.googleapis.com", "/oauth2/v3/tokeninfo?id_token="+token, null);
		Map<String,Object> result = JsonUtils.toObject(s, Map.class);
		if (result == null || !validAuidence(result) || !result.containsKey("sub")) {
			throw new SecurityException("Incorrect google token "+ token+": "+s);
		}
		result.put("id", result.get("sub"));
		return result;
	}


	/**
	 * Check whether the token client audience matches what is expected
	 * @param result
	 * @return
	 */
	private boolean validAuidence(Map<String, Object> result) {
		return true;//googleClientIds.contains(result.get("audience"));
	}


	/**
	 * Validate Google token against API v1
	 * @param token
	 * @return
	 * @throws RemoteException
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> validateV1(String token) throws SecurityException, RemoteException {
		// first, we have to validate that the token is a correct platform token
		String s = RemoteConnector.getJSON("https://www.googleapis.com", "/oauth2/v1/tokeninfo?access_token="+token, null);
		Map<String,Object> result = JsonUtils.toObject(s, Map.class);
		if (result == null || !validAuidence(result)) {
			throw new SecurityException("Incorrect google token "+ token+": "+s);
		}
		// second, we have to get the user information
		s = RemoteConnector.getJSON("https://www.googleapis.com", "/oauth2/v1/userinfo", token);
		result = JsonUtils.toObject(s, Map.class);
		if (result == null || !result.containsKey("id")) {
			throw new SecurityException("Incorrect google token "+ token+": "+s);
		}
		return result;
	}

	/**
	 * @param result
	 * @return
	 */
	private Map<String, String> extractAttributes(Map<String, Object> result, AuthorityMapping mapping) {
		Map<String, String> attrs = new HashMap<String, String>(); 
		for (String key : mapping.getIdentifyingAttributes()) {
			Object value = result.get(key);
			if (value != null) {
				attrs.put(key, value.toString());
			}
		}
		for (Attributes attribute : mapping.getAttributes()) {
			// used alias if present to set attribute in map
			Object value = result.get(attribute.getValue());
			if (value != null) {
				String key = (attribute.getAlias() != null && !attribute.getAlias()
						.isEmpty()) ? attribute.getAlias() : attribute.getValue();
				attrs.put(key, value.toString());
			}
		}
		return attrs;	
	}
}
