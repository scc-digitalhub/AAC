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

package it.smartcommunitylab.aac.model;

import java.util.Map;
import java.util.Set;

/**
 * Application descriptor.
 * @author raman
 *
 */
public class ClientAppBasic {

	/** GRANT TYPE: IMPLICIT FLOW */
	private static final String GT_IMPLICIT = "implicit";
	/** GRANT TYPE: AUTHORIZATION GRANT FLOW */
	private static final String GT_AUTHORIZATION_CODE = "authorization_code";

	private String clientId;
	private String clientSecret;
	private String clientSecretMobile;
	private String name;
	private String redirectUris;
	private Set<String> grantedTypes;

	private boolean nativeAppsAccess;
	private String nativeAppSignatures;

	private Map<String,Boolean> identityProviders;
	private Map<String,Boolean> identityProviderApproval;

	private String userName;
	private String scope;
	
	private String parameters;
	
	/**
	 * @return the clientId
	 */
	public String getClientId() {
		return clientId;
	}
	/**
	 * @param clientId the clientId to set
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	/**
	 * @return the clientSecret
	 */
	public String getClientSecret() {
		return clientSecret;
	}
	/**
	 * @param clientSecret the clientSecret to set
	 */
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the redirectUris
	 */
	public String getRedirectUris() {
		return redirectUris;
	}
	/**
	 * @param redirectUris the redirectUris to set
	 */
	public void setRedirectUris(String redirectUris) {
		this.redirectUris = redirectUris;
	}
	/**
	 * @return the grantedTypes
	 */
	public Set<String> getGrantedTypes() {
		return grantedTypes;
	}
	/**
	 * @param set the grantedTypes to set
	 */
	public void setGrantedTypes(Set<String> set) {
		this.grantedTypes = set;
	}
	/**
	 * @return the nativeAppsAccess
	 */
	public boolean isNativeAppsAccess() {
		return nativeAppsAccess;
	}
	/**
	 * @param nativeAppsAccess the nativeAppsAccess to set
	 */
	public void setNativeAppsAccess(boolean nativeAppsAccess) {
		this.nativeAppsAccess = nativeAppsAccess;
	}
	/**
	 * @return the nativeAppSignatures
	 */
	public String getNativeAppSignatures() {
		return nativeAppSignatures;
	}
	/**
	 * @param nativeAppSignatures the nativeAppSignatures to set
	 */
	public void setNativeAppSignatures(String nativeAppSignatures) {
		this.nativeAppSignatures = nativeAppSignatures;
	}
	/**
	 * @return the clientSecretMobile
	 */
	public String getClientSecretMobile() {
		return clientSecretMobile;
	}
	/**
	 * @param clientSecretMobile the clientSecretMobile to set
	 */
	public void setClientSecretMobile(String clientSecretMobile) {
		this.clientSecretMobile = clientSecretMobile;
	}
	/**
	 * @return the identityProviders
	 */
	public Map<String, Boolean> getIdentityProviders() {
		return identityProviders;
	}
	/**
	 * @param identityProviders the identityProviders to set
	 */
	public void setIdentityProviders(Map<String, Boolean> identityProviders) {
		this.identityProviders = identityProviders;
	}
	/**
	 * @return the identityProviderApproval
	 */
	public Map<String, Boolean> getIdentityProviderApproval() {
		return identityProviderApproval;
	}
	/**
	 * @param identityProviderApproval the identityProviderApproval to set
	 */
	public void setIdentityProviderApproval(
			Map<String, Boolean> identityProviderApproval) {
		this.identityProviderApproval = identityProviderApproval;
	}
	/**
	 * @return
	 */
	public boolean hasServerSideAccess() {
		return grantedTypes != null && (grantedTypes.contains(GT_IMPLICIT) || grantedTypes.contains(GT_AUTHORIZATION_CODE));
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getScope() {
		return scope;
	}
	public void setScope(String scope) {
		this.scope = scope;
	}
	public String getParameters() {
		return parameters;
	}
	public void setParameters(String parameters) {
		this.parameters = parameters;
	}
}
