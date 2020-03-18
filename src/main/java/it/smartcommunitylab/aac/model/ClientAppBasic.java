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

package it.smartcommunitylab.aac.model;

import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;

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
	private String name, displayName;
	private Set<String> redirectUris;
	private Set<String> grantedTypes;

	private boolean nativeAppsAccess;
	private Map<String, Map<String, Object>> providerConfigurations;
	private String mobileAppSchema;

	private Map<String,Boolean> identityProviders;
	private Map<String,Boolean> identityProviderApproval;

	private Set<String> uniqueSpaces;
	private String claimMapping; 
	private String onAfterApprovalWebhook;
	
	private String userName;
	private Set<String> scope;
	
	private String parameters;
	
	private Integer accessTokenValidity;
	private Integer refreshTokenValidity;
	
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
//	/**
//	 * @return the redirectUris
//	 */
//	public String getRedirectUris() {
//		return redirectUris;
//	}
//	/**
//	 * @param redirectUris the redirectUris to set
//	 */
//	public void setRedirectUris(String redirectUris) {
//		this.redirectUris = redirectUris;
//	}
	
	/**
	 * @return the grantedTypes
	 */
	public Set<String> getGrantedTypes() {
		return grantedTypes;
	}
	public Set<String> getRedirectUris() {
        return redirectUris;
    }
    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
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
	
	public String getDisplayName() {
		return StringUtils.isEmpty(displayName) ? name : displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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
//	public String getScope() {
//		return scope;
//	}
//	public void setScope(String scope) {
//		this.scope = scope;
//	}
	
	public String getParameters() {
		return parameters;
	}
	public Set<String> getScope() {
        return scope;
    }
    public void setScope(Set<String> scope) {
        this.scope = scope;
    }
    public void setParameters(String parameters) {
		this.parameters = parameters;
	}
	public Map<String, Map<String, Object>> getProviderConfigurations() {
		return providerConfigurations;
	}
	public void setProviderConfigurations(Map<String, Map<String, Object>> providerConfigurations) {
		this.providerConfigurations = providerConfigurations;
	}
	public String getMobileAppSchema() {
		return mobileAppSchema;
	}
	public void setMobileAppSchema(String nativeAppSchema) {
		this.mobileAppSchema = nativeAppSchema;
	}
	/**
	 * @return the uniqueSpaces
	 */
	public Set<String> getUniqueSpaces() {
		return uniqueSpaces;
	}
	/**
	 * @param uniqueSpaces the uniqueSpaces to set
	 */
	public void setUniqueSpaces(Set<String> uniqueSpaces) {
		this.uniqueSpaces = uniqueSpaces;
	}
	/**
	 * @return the claimMapping
	 */
	public String getClaimMapping() {
		return claimMapping;
	}
	/**
	 * @param claimMapping the claimMapping to set
	 */
	public void setClaimMapping(String claimMapping) {
		this.claimMapping = claimMapping;
	}
	/**
	 * @return the onAfterApprovalWebhook
	 */
	public String getOnAfterApprovalWebhook() {
		return onAfterApprovalWebhook;
	}
	/**
	 * @param onAfterApprovalWebhook the onAfterApprovalWebhook to set
	 */
	public void setOnAfterApprovalWebhook(String onAfterApprovalWebhook) {
		this.onAfterApprovalWebhook = onAfterApprovalWebhook;
	}
    public Integer getAccessTokenValidity() {
        return accessTokenValidity;
    }
    public void setAccessTokenValidity(int accessTokenValidity) {
        this.accessTokenValidity = Integer.valueOf(accessTokenValidity);
    }
    public Integer getRefreshTokenValidity() {
        return refreshTokenValidity;
    }
    public void setRefreshTokenValidity(int refreshTokenValidity) {
        this.refreshTokenValidity = Integer.valueOf(refreshTokenValidity);
    }
	
	
}
