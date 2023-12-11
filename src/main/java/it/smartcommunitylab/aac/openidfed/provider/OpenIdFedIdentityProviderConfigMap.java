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

package it.smartcommunitylab.aac.openidfed.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.nimbusds.openid.connect.sdk.SubjectType;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.oauth.model.PromptMode;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenIdFedIdentityProviderConfigMap extends AbstractConfigMap {

    private static final long serialVersionUID = SystemKeys.AAC_OPENIDFED_SERIAL_VERSION;

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_IDENTITY_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_OPENIDFED;

    //client settings
    private String clientId;
    private String clientJwks;
    private String clientName;

    private String scope;

    // principal settings
    private String userNameAttributeName;
    private Boolean trustEmailAddress;
    private Boolean requireEmailAddress;
    private Boolean alwaysTrustEmailAddress;

    // session control
    private Boolean respectTokenExpiration;
    private Set<PromptMode> promptMode;

    // openid-fed
    private String trustAnchor;

    private Set<EntityID> providers;

    private String federationJwks;
    private Set<String> acrValues;
    private Set<String> authorityHints;
    private String trustMarks;
    private SubjectType subjectType;

    private String organizationName;
    private List<String> contacts;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientJwks() {
        return clientJwks;
    }

    public void setClientJwks(String clientJwks) {
        this.clientJwks = clientJwks;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
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

    public Boolean getTrustEmailAddress() {
        return trustEmailAddress;
    }

    public void setTrustEmailAddress(Boolean trustEmailAddress) {
        this.trustEmailAddress = trustEmailAddress;
    }

    public Boolean getRequireEmailAddress() {
        return requireEmailAddress;
    }

    public void setRequireEmailAddress(Boolean requireEmailAddress) {
        this.requireEmailAddress = requireEmailAddress;
    }

    public Boolean getAlwaysTrustEmailAddress() {
        return alwaysTrustEmailAddress;
    }

    public void setAlwaysTrustEmailAddress(Boolean alwaysTrustEmailAddress) {
        this.alwaysTrustEmailAddress = alwaysTrustEmailAddress;
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

    public String getTrustAnchor() {
        return trustAnchor;
    }

    public void setTrustAnchor(String trustAnchor) {
        this.trustAnchor = trustAnchor;
    }

    public Set<EntityID> getProviders() {
        return providers;
    }

    public void setProviders(Set<EntityID> providers) {
        this.providers = providers;
    }

    public String getFederationJwks() {
        return federationJwks;
    }

    public void setFederationJwks(String federationJwks) {
        this.federationJwks = federationJwks;
    }

    public Set<String> getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(Set<String> acrValues) {
        this.acrValues = acrValues;
    }

    public Set<String> getAuthorityHints() {
        return authorityHints;
    }

    public void setAuthorityHints(Set<String> authorityHints) {
        this.authorityHints = authorityHints;
    }

    public String getTrustMarks() {
        return trustMarks;
    }

    public void setTrustMarks(String trustMarks) {
        this.trustMarks = trustMarks;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    @JsonIgnore
    public void setConfiguration(final OpenIdFedIdentityProviderConfigMap map) {
        this.clientId = map.getClientId();
        this.clientJwks = map.getClientJwks();
        this.clientName = map.getClientName();

        this.scope = map.getScope();

        this.userNameAttributeName = map.getUserNameAttributeName();
        this.trustEmailAddress = map.getTrustEmailAddress();
        this.alwaysTrustEmailAddress = map.getAlwaysTrustEmailAddress();
        this.requireEmailAddress = map.getRequireEmailAddress();

        this.respectTokenExpiration = map.getRespectTokenExpiration();
        this.promptMode = map.getPromptMode();

        this.trustAnchor = map.getTrustAnchor();
        this.providers = map.getProviders();
        this.federationJwks = map.getFederationJwks();
        this.acrValues = map.getAcrValues();
        this.authorityHints = map.getAuthorityHints();
        this.trustMarks = map.getTrustMarks();
        this.subjectType = map.getSubjectType();

        this.organizationName = map.getOrganizationName();
        this.contacts = map.getContacts();
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        OpenIdFedIdentityProviderConfigMap map = mapper.convertValue(props, OpenIdFedIdentityProviderConfigMap.class);

        setConfiguration(map);
    }

    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(OpenIdFedIdentityProviderConfigMap.class);
    }
}
