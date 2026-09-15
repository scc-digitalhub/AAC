/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidAuthnContext;
import it.smartcommunitylab.aac.spid.model.SpidPurpose;
import it.smartcommunitylab.aac.spid.model.SpidUserAttribute;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpidIdentityProviderConfigMap extends AbstractConfigMap implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_IDENTITY_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_SPID;

    // <Signature> options
    private List<SigningCredential> signingCredentials;
    private String activeAuthRequestSigningCredentialId;
    private String activeMetadataSigningCredentialId;

    private String signingKey;
    private String signingCertificate;

    private String metadataUrl;
    private String metadataXML;
    
    private String entityId;
    private String organizationName;
    private String organizationDisplayName;
    private String organizationUrl;

    @Pattern(regexp = SystemKeys.EMAIL_PATTERN)
    private String contactPersonEmailAddress;
    @Pattern(regexp = "^[A-Za-z0-9_]*$")
    private String contactPersonIPACode;

    private Set<SpidAttribute> spidAttributes;

    // AAC options
    // NOTE: only one among {idps, idpMetadataUrl} can be non null
    // NOTE: idps is intended to be a subset of the local SPID registry. Both idps and idpMetadataUrl are intended
    //  to be used for testing purposes. If both are null, the full local SPID registry will be used instead.
    private Set<String> idps; // optional (see note above)
    private String idpMetadataUrl; // optional (see note above)

    private Boolean useAssertionConsumerServiceUrl;
    private Integer attributeConsumingServiceIndex;
    private SpidAuthnContext authnContext;
    private SpidPurpose purpose; // optional: null means classic SPID (identity types 1 and 2 only)

    private SpidUserAttribute subAttributeName; // optional
    private SpidUserAttribute usernameAttributeName; // optional

    public List<SigningCredential> getSigningCredentials() {
        return signingCredentials;
    }

    public void setSigningCredentials(List<SigningCredential> signingCredentials) {
        this.signingCredentials = signingCredentials;
    }

    public String getSigningKey() {
        return signingKey;
    }

    public void setSigningKey(String signingKey) {
        this.signingKey = signingKey;
    }

    public String getSigningCertificate() {
        return signingCertificate;
    }

    public void setSigningCertificate(String signingCertificate) {
        this.signingCertificate = signingCertificate;
    }

    public String getActiveAuthRequestSigningCredentialId() {
        return activeAuthRequestSigningCredentialId;
    }

    public void setActiveAuthRequestSigningCredentialId(String activeAuthRequestSigningCredentialId) {
        this.activeAuthRequestSigningCredentialId = activeAuthRequestSigningCredentialId;
    }

    public String getActiveMetadataSigningCredentialId() { return activeMetadataSigningCredentialId; }

    public void setActiveMetadataSigningCredentialId(String activeMetadataSigningCredentialId) {
        this.activeMetadataSigningCredentialId = activeMetadataSigningCredentialId;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    public String getMetadataXML() {
        return metadataXML;
    }

    public void setMetadataXML(String metadataXML) {
        this.metadataXML = metadataXML;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationDisplayName() {
        return organizationDisplayName;
    }

    public void setOrganizationDisplayName(String organizationDisplayName) {
        this.organizationDisplayName = organizationDisplayName;
    }

    public String getOrganizationUrl() {
        return organizationUrl;
    }

    public void setOrganizationUrl(String organizationUrl) {
        this.organizationUrl = organizationUrl;
    }

    public String getContactPersonEmailAddress() {
        return contactPersonEmailAddress;
    }

    public void setContactPersonEmailAddress(String contactPersonEmailAddress) {
        this.contactPersonEmailAddress = contactPersonEmailAddress;
    }

    public String getContactPersonIPACode() {
        return contactPersonIPACode;
    }

    public void setContactPersonIPACode(String contactPersonIPACode) {
        this.contactPersonIPACode = contactPersonIPACode;
    }

    public Set<SpidAttribute> getSpidAttributes() {
        return spidAttributes;
    }

    public void setSpidAttributes(Set<SpidAttribute> spidAttributes) {
        this.spidAttributes = spidAttributes;
    }

    public Set<String> getIdps() {
        return idps;
    }

    public void setIdps(Set<String> idps) {
        this.idps = idps;
    }

    public String getIdpMetadataUrl() {
        return idpMetadataUrl;
    }

    public void setIdpMetadataUrl(String idpMetadataUrl) {
        this.idpMetadataUrl = idpMetadataUrl;
    }

    public Boolean getUseAssertionConsumerServiceUrl() {
        return useAssertionConsumerServiceUrl;
    }

    public void setUseAssertionConsumerServiceUrl(Boolean useAssertionConsumerServiceUrl) {
        this.useAssertionConsumerServiceUrl = useAssertionConsumerServiceUrl;
    }

    public Integer getAttributeConsumingServiceIndex() {
        return attributeConsumingServiceIndex;
    }

    public void setAttributeConsumingServiceIndex(Integer attributeConsumingServiceIndex) {
        this.attributeConsumingServiceIndex = attributeConsumingServiceIndex;
    }

    public SpidAuthnContext getAuthnContext() {
        return authnContext;
    }

    public void setAuthnContext(SpidAuthnContext authnContext) {
        this.authnContext = authnContext;
    }

    public SpidPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(SpidPurpose purpose) {
        this.purpose = purpose;
    }

    public SpidUserAttribute getSubAttributeName() {
        return this.subAttributeName;
    }

    public void setSubAttributeName(SpidUserAttribute subAttributeName) {
        this.subAttributeName = subAttributeName;
    }

    public SpidUserAttribute getUsernameAttributeName() {
        return usernameAttributeName;
    }

    public void setUsernameAttributeName(SpidUserAttribute usernameAttributeName) {
        this.usernameAttributeName = usernameAttributeName;
    }

    @JsonIgnore
    public void setConfiguration(SpidIdentityProviderConfigMap map) {
        this.signingKey = map.getSigningKey();
        this.signingCertificate = map.getSigningCertificate();
        this.signingCredentials = map.getSigningCredentials();
        this.activeAuthRequestSigningCredentialId = map.getActiveAuthRequestSigningCredentialId();
        this.activeMetadataSigningCredentialId = map.getActiveMetadataSigningCredentialId();
        this.metadataUrl = map.getMetadataUrl();
        this.metadataXML = map.getMetadataXML();
        this.entityId = map.getEntityId();
        this.organizationName = map.getOrganizationName();
        this.organizationDisplayName = map.getOrganizationDisplayName();
        this.organizationUrl = map.getOrganizationUrl();
        this.contactPersonEmailAddress = map.getContactPersonEmailAddress();
        this.contactPersonIPACode = map.getContactPersonIPACode();
        this.spidAttributes = map.getSpidAttributes();
        this.idps = map.getIdps();
        this.idpMetadataUrl = map.getIdpMetadataUrl();
        this.useAssertionConsumerServiceUrl = map.getUseAssertionConsumerServiceUrl();
        this.attributeConsumingServiceIndex = map.getAttributeConsumingServiceIndex();
        this.authnContext = map.getAuthnContext();
        this.purpose = map.getPurpose();
        this.subAttributeName = map.getSubAttributeName();
        this.usernameAttributeName = map.getUsernameAttributeName();
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        SpidIdentityProviderConfigMap map = mapper.convertValue(props, SpidIdentityProviderConfigMap.class);

        setConfiguration(map);
    }

    @Override
    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(SpidIdentityProviderConfigMap.class);
    }
}
