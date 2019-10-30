/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
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
 */

package it.smartcommunitylab.aac;

/**
 * Constants and methods for managing resource visibility
 * @author raman
 *
 */
public class Config {

	public static final String DEFAULT_LANG = "en";
	
	/** User name attribute alias */
	public static final String NAME_ATTR = "it.smartcommunitylab.aac.givenname";
	/** User surname attribute alias */
	public static final String SURNAME_ATTR = "it.smartcommunitylab.aac.surname";
	/** User surname attribute alias */
	public static final String USERNAME_ATTR = "it.smartcommunitylab.aac.username";
	/** Internal Attribute Authority */ 
	public static final String IDP_INTERNAL = "internal";
	
	/** Authorization authorities */
	public enum AUTHORITY {ROLE_USER, ROLE_CLIENT, ROLE_ANY, ROLE_CLIENT_TRUSTED};
	
	/** Resource visibility values: either only the specific app can see, or all the apps of the current developer, or any app */
	public enum RESOURCE_VISIBILITY {CLIENT_APP,DEVELOPER,PUBLIC}

	/** Requesting device type */
	public enum DEVICE_TYPE {MOBILE, TABLET, DESKTOP, UNKNOWN, WEBVIEW};
	
	/** operation scope requiring strong 2-factor authentication */
	public static final String SCOPE_OPERATION_CONFIRMED = "operation.confirmed";
	
	/** Session attribute holding AAC OAuth2 request context */
	public static final String SESSION_ATTR_AAC_OAUTH_REQUEST = "aacOAuthRequest";

	public static final String PARAM_REMEMBER_ME= "remember-me";
	public static final String COOKIE_REMEMBER_ME= "aacrememberme";

	
	/** Predefined system role USER */
	public static final String R_USER = "ROLE_USER";
	/** Predefined system role ADMIN */
	public static final String R_ADMIN = "ROLE_ADMIN";
	/** Predefined system role R_PROVIDER */
	public static final String R_PROVIDER = "ROLE_PROVIDER";

	public static final String GRANT_TYPE_NATIVE = "native";

	/** Open ID connect scope */
	public static final String OPENID_SCOPE = "openid";
	/** Predefined scope for user creation and management */
	public static final String SCOPE_USERMANAGEMENT = "usermanagement";
	public static final String ACCOUNT_PROFILE_SCOPE = "profile.accountprofile.me";
	public static final String BASIC_PROFILE_SCOPE = "profile.basicprofile.me";
	public static final String EMAIL_SCOPE = "email";

	public static final String CLIENT_PARAM_SIGNED_RESPONSE_ALG = "signed_response_alg";
	public static final String CLIENT_PARAM_ENCRYPTED_RESPONSE_ALG = "encrypted_response_alg";
	public static final String CLIENT_PARAM_ENCRYPTED_RESPONSE_ENC = "encrypted_response_enc";
	public static final String CLIENT_PARAM_JWKS = "jwks";
	public static final String CLIENT_PARAM_JWKS_URI = "jwks_uri";


	

	/**
	 * Check whether the child property visibility is equal or more restrictive than the one of the parent property.
	 * @param parentVis
	 * @param childVis
	 */
	public static boolean checkVisibility(RESOURCE_VISIBILITY parentVis, RESOURCE_VISIBILITY childVis) {
		switch (childVis) {
		case DEVELOPER:
			return parentVis != RESOURCE_VISIBILITY.CLIENT_APP;
		case PUBLIC:
			return parentVis == RESOURCE_VISIBILITY.PUBLIC;
		default:
			return true;
		}
	}

	/**
	 * @param parentVis
	 * @param childVis
	 * @return the most restrictive visibility of the two 
	 */
	public static RESOURCE_VISIBILITY alignVisibility(RESOURCE_VISIBILITY parentVis, RESOURCE_VISIBILITY childVis) {
		return checkVisibility(parentVis, childVis) ? childVis : parentVis;
	};
}
