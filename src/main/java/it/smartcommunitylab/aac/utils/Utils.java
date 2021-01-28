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

package it.smartcommunitylab.aac.utils;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.AACAuthenticationToken;
import it.smartcommunitylab.aac.oauth.AACOAuthRequest;

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
	 * URL for authentication filters of identity providers
	 * @param provider
	 * @return
	 */
	public static String filterRedirectURL(String provider) {
		return "/auth/"+provider+"-oauth/callback";
	}
	
//	public static String extractUserFromTenant(String tenant) {
//		String un = tenant;
//		
//		int index = un.indexOf('@');
//		int lastIndex = un.lastIndexOf('@');
//		
//		if (index != lastIndex) {
//			un = un.substring(0, lastIndex);
//		} else if (un.endsWith("@carbon.super")) {
//			un = un.substring(0, un.indexOf('@'));
//		}
//		
//		return un;
//	}
//	public static String getUserNameAtTenant(String username, String tenantName) {
//		return username + "@" + tenantName;
//	}
//	
//	public static String[] extractInfoFromTenant(String tenant) {
//		int index = tenant.indexOf('@');
//		int lastIndex = tenant.lastIndexOf('@');
//		
//		if (index != lastIndex) {
//			String result[] = new String[2];
//			result[0] = tenant.substring(0, lastIndex);
//			result[1] = tenant.substring(lastIndex + 1, tenant.length());
//			return result;
//		} else if (tenant.endsWith("@carbon.super")) {
//			return tenant.split("@");
//		}
//		return new String[] {tenant, "carbon.super"};
//	}	
	
//	public static String parseHeaderToken(HttpServletRequest request) {
//		Enumeration<String> headers = request.getHeaders("Authorization");
//		while (headers.hasMoreElements()) { // typically there is only one (most servers enforce that)
//			String value = headers.nextElement();
//			if ((value.toLowerCase().startsWith(OAuth2AccessToken.BEARER_TYPE.toLowerCase()))) {
//				String authHeaderValue = value.substring(OAuth2AccessToken.BEARER_TYPE.length()).trim();
//				int commaIndex = authHeaderValue.indexOf(',');
//				if (commaIndex > 0) {
//					authHeaderValue = authHeaderValue.substring(0, commaIndex);
//				}
//				return authHeaderValue;
//			}
//		}
//
//		return null;
//	}		
	
	
	/*
	 * User helpers
	 */
	   /**
     * Get the user from the Spring Security Context
     * @return
     * @throws AccessDeniedException 
     */
    public static UserDetails getUserDetails() throws AccessDeniedException{
        try {
            return (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) {
            throw new AccessDeniedException("Incorrect user");
        }
    }
    
    /**
     * @return the user ID (long) from the user object in Spring Security Context
     */
    public static Long getUserId() throws AccessDeniedException {
        try {
            return Long.parseLong(getUserDetails().getUsername());
        } catch (Exception e) {
            throw new AccessDeniedException("Incorrect username format");
        }
    }

    /**
     * The authority (e.g., google) value from the Spring Security Context of the currently logged user
     * @return the authority value (string)
     */
    public static String getUserAuthority() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AACAuthenticationToken) {
            AACAuthenticationToken aacToken = (AACAuthenticationToken)authentication;
            AACOAuthRequest request = (AACOAuthRequest) aacToken.getDetails();
            return request.getAuthority();
        }
        return null;
    }

    /**
     * The authority (e.g., google) value from the Spring Security Context of the currently logged user
     * @return the authority value (string)
     */
    public static Set<String> getUserRoles() {
        Set<String> res = new HashSet<>();
        SecurityContextHolder.getContext().getAuthentication().getAuthorities().forEach(ga -> res.add(ga.getAuthority()));
        return res;
    }

    
}
