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
 * 
 * @author raman
 *
 */
public class ClientAppBasic {

    private String clientId;
    private String clientSecret;
    private String name, displayName;
    private Set<String> redirectUris;
    private Set<String> grantedTypes;

    private Map<String, Map<String, Object>> providerConfigurations;
    private String mobileAppSchema;

    private Map<String, Boolean> identityProviders;
    private Map<String, Boolean> identityProviderApproval;

    private Set<String> uniqueSpaces;
    private String claimMapping;
    private String onAfterApprovalWebhook;
    private Set<String> rolePrefixes;

    private String userName;
    private Set<String> scope;

    private String parameters;

    private Integer accessTokenValidity;
    private Integer refreshTokenValidity;

    private String jwtSignAlgorithm;
    private String jwtEncAlgorithm;
    private String jwtEncMethod;
    private String jwks;
    private String jwksUri;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getGrantedTypes() {
        return grantedTypes;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public void setGrantedTypes(Set<String> set) {
        this.grantedTypes = set;
    }

    public Map<String, Boolean> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(Map<String, Boolean> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public Map<String, Boolean> getIdentityProviderApproval() {
        return identityProviderApproval;
    }

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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

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

    public Set<String> getUniqueSpaces() {
        return uniqueSpaces;
    }

    public void setUniqueSpaces(Set<String> uniqueSpaces) {
        this.uniqueSpaces = uniqueSpaces;
    }

    public String getClaimMapping() {
        return claimMapping;
    }

    public void setClaimMapping(String claimMapping) {
        this.claimMapping = claimMapping;
    }

    public String getOnAfterApprovalWebhook() {
        return onAfterApprovalWebhook;
    }

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

    public Set<String> getRolePrefixes() {
        return rolePrefixes;
    }

    public void setRolePrefixes(Set<String> rolePrefixes) {
        this.rolePrefixes = rolePrefixes;
    }

    public String getJwtSignAlgorithm() {
        return jwtSignAlgorithm;
    }

    public void setJwtSignAlgorithm(String jwtSignAlgorithm) {
        this.jwtSignAlgorithm = jwtSignAlgorithm;
    }

    public String getJwtEncAlgorithm() {
        return jwtEncAlgorithm;
    }

    public void setJwtEncAlgorithm(String jwtEncAlgorithm) {
        this.jwtEncAlgorithm = jwtEncAlgorithm;
    }

    public String getJwtEncMethod() {
        return jwtEncMethod;
    }

    public void setJwtEncMethod(String jwtEncMethod) {
        this.jwtEncMethod = jwtEncMethod;
    }

    public String getJwks() {
        return jwks;
    }

    public void setJwks(String jwks) {
        this.jwks = jwks;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public void setAccessTokenValidity(Integer accessTokenValidity) {
        this.accessTokenValidity = accessTokenValidity;
    }

    public void setRefreshTokenValidity(Integer refreshTokenValidity) {
        this.refreshTokenValidity = refreshTokenValidity;
    }

}
