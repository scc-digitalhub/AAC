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

package it.smartcommunitylab.aac.common;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;

import it.smartcommunitylab.aac.jaxbmodel.ResourceMapping;
import it.smartcommunitylab.aac.model.ServiceDescriptor;

/**
 * Common methods and functions
 * @author raman
 *
 */
public class Utils {

	/**
	 * Generate set of strings out of specified delimited string. Remove also leading/trailing spaces around the elements.
	 * @param input
	 * @param delimiter
	 * @return
	 */
	public static Set<String> delimitedStringToSet(String input, String delimiter) {
		HashSet<String> res = new HashSet<String>();
		String[] arr = null;
		if (delimiter != null) {
			arr = input.split(delimiter);
			for (String s : arr) {
				res.add(s.trim());
			}
		}
		return res;
	}
	
	/**
	 * Correct values of the specified comma-separated string: remove redundant spaces
	 * @param in
	 * @return
	 */
	public static String normalizeValues(String in) {
		return StringUtils.trimAllWhitespace(in);
	}
	
	/**
	 * Convert {@link it.smartcommunitylab.aac.jaxbmodel.Service} object
	 * to {@link ServiceDescriptor} persisted entity
	 * @param s
	 * @return converted {@link ServiceDescriptor} entity
	 */
	public static ServiceDescriptor toServiceEntity(it.smartcommunitylab.aac.jaxbmodel.Service s) {
		ObjectMapper mapper = new ObjectMapper();
		ServiceDescriptor res = new ServiceDescriptor();
		res.setDescription(s.getDescription());
		res.setServiceName(s.getName());
		res.setServiceId(s.getId());
		try {
			res.setResourceMappings(mapper.writeValueAsString(s.getResourceMapping()));
		} catch (JsonProcessingException e) {
		}
		res.setApiKey(s.getApiKey());
		return res;
	} 
	/**
	 * Convert {@link ServiceDescriptor} entity to {@link it.smartcommunitylab.aac.jaxbmodel.Service} object
	 * @param s
	 * @return converted {@link it.smartcommunitylab.aac.jaxbmodel.Service} object
	 */
	public static it.smartcommunitylab.aac.jaxbmodel.Service toServiceObject(ServiceDescriptor s) {
		it.smartcommunitylab.aac.jaxbmodel.Service res = new it.smartcommunitylab.aac.jaxbmodel.Service();
		res.setDescription(s.getDescription());
		res.setId(s.getServiceId());
		res.setName(s.getServiceName());
		res.setApiKey(s.getApiKey());
		res.getResourceMapping().clear();
		List<ResourceMapping> resourceMapping = toObjectList(s.getResourceMappings(), ResourceMapping.class);
		res.getResourceMapping().addAll(resourceMapping);
		return res;
	} 
	
	/**
	 * URL for authentication filters of identity providers
	 * @param provider
	 * @return
	 */
	public static String filterRedirectURL(String provider) {
		return "/auth/"+provider+"-oauth/callback";
	}
	
	public static String extractUserFromTenant(String tenant) {
		String un = tenant;
		
		int index = un.indexOf('@');
		int lastIndex = un.lastIndexOf('@');
		
		if (index != lastIndex) {
			un = un.substring(0, lastIndex);
		} else if (un.endsWith("@carbon.super")) {
			un = un.substring(0, un.indexOf('@'));
		}
		
		return un;
	}
	public static String getUserNameAtTenant(String username, String tenantName) {
		return username + "@" + tenantName;
	}
	
	public static String[] extractInfoFromTenant(String tenant) {
		int index = tenant.indexOf('@');
		int lastIndex = tenant.lastIndexOf('@');
		
		if (index != lastIndex) {
			String result[] = new String[2];
			result[0] = tenant.substring(0, lastIndex);
			result[1] = tenant.substring(lastIndex + 1, tenant.length());
			return result;
		} else if (tenant.endsWith("@carbon.super")) {
			return tenant.split("@");
		}
		return new String[] {tenant, "carbon.super"};
	}	
	
	public static String parseHeaderToken(HttpServletRequest request) {
		Enumeration<String> headers = request.getHeaders("Authorization");
		while (headers.hasMoreElements()) { // typically there is only one (most servers enforce that)
			String value = headers.nextElement();
			if ((value.toLowerCase().startsWith(OAuth2AccessToken.BEARER_TYPE.toLowerCase()))) {
				String authHeaderValue = value.substring(OAuth2AccessToken.BEARER_TYPE.length()).trim();
				int commaIndex = authHeaderValue.indexOf(',');
				if (commaIndex > 0) {
					authHeaderValue = authHeaderValue.substring(0, commaIndex);
				}
				return authHeaderValue;
			}
		}

		return null;
	}		
	

	/**
	 * Convert JSON array string to the list of objects of the specified class
	 * @param body
	 * @param cls
	 * @return
	 */
	private static <T> List<T> toObjectList(String body, Class<T> cls) {
	    ObjectMapper fullMapper = new ObjectMapper();
        fullMapper.setAnnotationIntrospector(NopAnnotationIntrospector.nopInstance());
        fullMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        fullMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        fullMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        fullMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

		try {
			List<Object> list = fullMapper.readValue(body, new TypeReference<List<?>>() { });
			List<T> result = new ArrayList<T>();
			for (Object o : list) {
				result.add(fullMapper.convertValue(o,cls));
			}
			return result;
		} catch (Exception e) {
			return null;
		}
	}
	
}
