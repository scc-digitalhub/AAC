/**
 *    Copyright 2012-2013 Trento RISE
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
	/** Internal Attribute Authority */ 
	public static final String IDP_INTERNAL = "internal";
	
	/** Authorization authorities */
	public enum AUTHORITY {ROLE_USER, ROLE_CLIENT, ROLE_ANY, ROLE_CLIENT_TRUSTED};
	
	/** Resource visibility values: either only the specific app can see, or all the apps of the current developer, or any app */
	public enum RESOURCE_VISIBILITY {CLIENT_APP,DEVELOPER,PUBLIC}

	/** ROLE SCOPES */ 
	public enum ROLE_SCOPE {
		system ("SCOPE_SYSTEM"), 
		application ("SCOPE_APPLICATION"),
		tenant ("SCOPE_TENANT"),
		user ("SCOPE_USER");
		
		private final String scopeName;

		private ROLE_SCOPE(String scopeName) {
			this.scopeName = scopeName;
		}
		
		public String scopeName(){
			return scopeName;
		}
	};
	
	/** Predefined system role USER */
	public static final String R_USER = "ROLE_USER";
	/** Predefined system role ADMIN */
	public static final String R_ADMIN = "ROLE_ADMIN";

	public static final String GRANT_TYPE_NATIVE = "native";

	/** Predefined scope for user creation and management */
	public static final Object SCOPE_USERMANAGEMENT = "usermanagement";

	

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
