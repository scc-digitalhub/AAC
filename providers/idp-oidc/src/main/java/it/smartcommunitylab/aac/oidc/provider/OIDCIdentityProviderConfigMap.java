/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.oidc.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.PromptMode;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class OIDCIdentityProviderConfigMap extends AbstractConfigMap {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_IDENTITY_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_OIDC;

    private String clientId;
    private String clientSecret;
    private String clientJwk;
    private String clientName;

    private AuthenticationMethod clientAuthenticationMethod;
    private Boolean enablePkce;

    private String scope;
    private String userNameAttributeName;
    private String subAttributeName;
    private Boolean trustEmailAddress;
    private Boolean requireEmailAddress;
    private Boolean alwaysTrustEmailAddress;

    // explicit config
    private String authorizationUri;
    private String tokenUri;
    private String jwkSetUri;
    private String userInfoUri;

    // autoconfiguration support from well-known
    private String issuerUri;

    // session control
    private Boolean propagateEndSession;
    private Boolean respectTokenExpiration;
    private Set<PromptMode> promptMode;

    public OIDCIdentityProviderConfigMap() {}

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

    public String getClientJwk() {
        return clientJwk;
    }

    public void setClientJwk(String clientJwk) {
        this.clientJwk = clientJwk;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public AuthenticationMethod getClientAuthenticationMethod() {
        return clientAuthenticationMethod;
    }

    public void setClientAuthenticationMethod(AuthenticationMethod clientAuthenticationMethod) {
        this.clientAuthenticationMethod = clientAuthenticationMethod;
    }

    public Boolean getEnablePkce() {
        return enablePkce;
    }

    public void setEnablePkce(Boolean enablePkce) {
        this.enablePkce = enablePkce;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getUserNameAttributeName() {
        return userNameAttributeName;
    }

    public void setUserNameAttributeName(String userNameAttributeName) {
        this.userNameAttributeName = userNameAttributeName;
    }

    public String getSubAttributeName() {
        return subAttributeName;
    }

    public void setSubAttributeName(String subAttributeName) {
        this.subAttributeName = subAttributeName;
    }

    public Boolean getTrustEmailAddress() {
        return trustEmailAddress;
    }

    public void setTrustEmailAddress(Boolean trustEmailAddress) {
        this.trustEmailAddress = trustEmailAddress;
    }

    public Boolean getAlwaysTrustEmailAddress() {
        return alwaysTrustEmailAddress;
    }

    public void setAlwaysTrustEmailAddress(Boolean alwaysTrustEmailAddress) {
        this.alwaysTrustEmailAddress = alwaysTrustEmailAddress;
    }

    public Boolean getRequireEmailAddress() {
        return requireEmailAddress;
    }

    public void setRequireEmailAddress(Boolean requireEmailAddress) {
        this.requireEmailAddress = requireEmailAddress;
    }

    public String getAuthorizationUri() {
        return authorizationUri;
    }

    public void setAuthorizationUri(String authorizationUri) {
        this.authorizationUri = authorizationUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getUserInfoUri() {
        return userInfoUri;
    }

    public void setUserInfoUri(String userInfoUri) {
        this.userInfoUri = userInfoUri;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public Boolean getPropagateEndSession() {
        return propagateEndSession;
    }

    public void setPropagateEndSession(Boolean propagateEndSession) {
        this.propagateEndSession = propagateEndSession;
    }

    public Boolean getRespectTokenExpiration() {
        return respectTokenExpiration;
    }

    public void setRespectTokenExpiration(Boolean respectTokenExpiration) {
        this.respectTokenExpiration = respectTokenExpiration;
    }

    public Set<PromptMode> getPromptMode() {
        return promptMode;
    }

    public void setPromptMode(Set<PromptMode> promptMode) {
        this.promptMode = promptMode;
    }

    @JsonIgnore
    public void setConfiguration(OIDCIdentityProviderConfigMap map) {
        this.clientId = map.getClientId();
        this.clientSecret = map.getClientSecret();
        this.clientJwk = map.getClientJwk();
        this.clientName = map.getClientName();

        this.clientAuthenticationMethod = map.getClientAuthenticationMethod();
        this.enablePkce = map.getEnablePkce();
        this.scope = map.getScope();
        this.userNameAttributeName = map.getUserNameAttributeName();
        this.subAttributeName = map.getSubAttributeName();
        this.trustEmailAddress = map.getTrustEmailAddress();
        this.alwaysTrustEmailAddress = map.getAlwaysTrustEmailAddress();
        this.requireEmailAddress = map.getRequireEmailAddress();

        // explicit config
        this.authorizationUri = map.getAuthorizationUri();
        this.tokenUri = map.getTokenUri();
        this.jwkSetUri = map.getJwkSetUri();
        this.userInfoUri = map.getUserInfoUri();

        // autoconfiguration support from well-known
        this.issuerUri = map.getIssuerUri();

        // session
        this.propagateEndSession = map.getPropagateEndSession();
        this.respectTokenExpiration = map.getRespectTokenExpiration();
        this.promptMode = map.getPromptMode();
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        OIDCIdentityProviderConfigMap map = mapper.convertValue(props, OIDCIdentityProviderConfigMap.class);

        setConfiguration(map);
    }

    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(OIDCIdentityProviderConfigMap.class);
    }
}
