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

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Additional app info descriptor
 * 
 * @author raman
 *
 */
public class ClientAppInfo {

    public static final int APPROVED = 1;
    public static final int REJECTED = 2;
    public static final int REQUESTED = 0;
    public static final Integer UNKNOWN = -1;

    private static ObjectMapper mapper = new ObjectMapper();

    private String name, displayName;
    private String headerText, footerText;
    private String loginText, registrationText, accessConfirmationText;
    private String infoUri;

    private Map<String, Boolean> scopeApprovals;

    private Map<String, Integer> identityProviders;

    // DEPRECATED: unused, TODO remove
//	private String scope;

    private Map<String, Map<String, Object>> providerConfigurations;

    private Set<String> uniqueSpaces;
    private String claimMapping;
    private String onAfterApprovalWebhook;
    private Set<String> rolePrefixes;

    private String jwtSignAlgorithm;
    private String jwtEncAlgorithm;
    private String jwtEncMethod;
    private String jwks;
    private String jwksUri;

    static {
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static ClientAppInfo convert(Map<String, Object> map) {
        return mapper.convertValue(map, ClientAppInfo.class);
    }

    // TODO make converter static
    public String toJson() throws IllegalArgumentException {
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Boolean> getScopeApprovals() {
        return scopeApprovals;
    }

    public void setScopeApprovals(Map<String, Boolean> scopeApprovals) {
        this.scopeApprovals = scopeApprovals;
    }

    public Map<String, Integer> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(Map<String, Integer> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public Map<String, Map<String, Object>> getProviderConfigurations() {
        return providerConfigurations;
    }

    public void setProviderConfigurations(Map<String, Map<String, Object>> providerConfigurations) {
        this.providerConfigurations = providerConfigurations;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getHeaderText() {
        return headerText;
    }

    public void setHeaderText(String headerText) {
        this.headerText = headerText;
    }

    public String getFooterText() {
        return footerText;
    }

    public void setFooterText(String footerText) {
        this.footerText = footerText;
    }

    public String getLoginText() {
        return loginText;
    }

    public void setLoginText(String loginText) {
        this.loginText = loginText;
    }

    public String getRegistrationText() {
        return registrationText;
    }

    public void setRegistrationText(String registrationText) {
        this.registrationText = registrationText;
    }

    public String getAccessConfirmationText() {
        return accessConfirmationText;
    }

    public void setAccessConfirmationText(String accessConfirmationText) {
        this.accessConfirmationText = accessConfirmationText;
    }

    public String getInfoUri() {
        return infoUri;
    }

    public void setInfoUri(String infoUri) {
        this.infoUri = infoUri;
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

}
