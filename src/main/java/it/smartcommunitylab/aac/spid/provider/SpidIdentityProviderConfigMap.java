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
import it.smartcommunitylab.aac.spid.model.SpidUserAttribute;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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

    // core
    private String entityId;

    // <Signature> options
    private String signingKey;

    private String signingCertificate; // for <KeyDescriptor use="signing"><KeyInfo>

    // <Organization> options (suggested but not mandatory)
    private String organizationDisplayName;

    private String organizationName;

    private String organizationUrl;

    // <ContactPerson> options, only public SP is currently supported
    @Pattern(regexp = SystemKeys.EMAIL_PATTERN)
    private String contactPerson_EmailAddress;

    @Pattern(regexp = "^[A-Za-z0-9_]*$")
    private String contactPerson_IPACode;

    private Boolean contactPerson_Public; // Public/Private

    private String contactPerson_Type; // "other" (mandatory), optionally includes "billing" (unless private SP, in which case "billing" is also mandatory)

    // AAC options
    // NOTE: only one among {idps, idpMetadataUrl} can be non null
    // NOTE: idps is intended to be a subset of the local SPID registry. Both idps and idpMetadataUrl are intended
    //  to be used for testing purposes. If both are null, the full local SPID registry will be used instead.
    private Set<String> idps; // optional (see note above)
    private String idpMetadataUrl; // optional (see note above)
    private Set<SpidAttribute> spidAttributes; // optional

    private SpidAuthnContext authnContext;

    private SpidUserAttribute subAttributeName; // optional
    private SpidUserAttribute usernameAttributeName; // optional

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
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

    public String getContactPerson_EmailAddress() {
        return contactPerson_EmailAddress;
    }

    public void setContactPerson_EmailAddress(String contactPerson_EmailAddress) {
        this.contactPerson_EmailAddress = contactPerson_EmailAddress;
    }

    public String getContactPerson_IPACode() {
        return contactPerson_IPACode;
    }

    public void setContactPerson_IPACode(String contactPerson_IPACode) {
        this.contactPerson_IPACode = contactPerson_IPACode;
    }

    public Boolean getContactPerson_Public() {
        return contactPerson_Public;
    }

    public void setContactPerson_Public(Boolean contactPerson_Public) {
        this.contactPerson_Public = contactPerson_Public;
    }

    public String getContactPerson_Type() {
        return contactPerson_Type;
    }

    public void setContactPerson_Type(String contactPerson_Type) {
        this.contactPerson_Type = contactPerson_Type;
    }

    public String getOrganizationDisplayName() {
        return organizationDisplayName;
    }

    public void setOrganizationDisplayName(String organizationDisplayName) {
        this.organizationDisplayName = organizationDisplayName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationUrl() {
        return organizationUrl;
    }

    public void setOrganizationUrl(String organizationUrl) {
        this.organizationUrl = organizationUrl;
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

    public Set<SpidAttribute> getSpidAttributes() {
        return spidAttributes;
    }

    public void setSpidAttributes(Set<SpidAttribute> spidAttributes) {
        this.spidAttributes = spidAttributes;
    }

    public SpidAuthnContext getAuthnContext() {
        return authnContext;
    }

    public void setAuthnContext(SpidAuthnContext authnContext) {
        this.authnContext = authnContext;
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
        this.entityId = map.getEntityId();
        this.signingKey = map.getSigningKey();
        this.signingCertificate = map.getSigningKey();
        this.contactPerson_EmailAddress = map.getContactPerson_EmailAddress();
        this.contactPerson_IPACode = map.getContactPerson_IPACode();
        this.contactPerson_Public = map.getContactPerson_Public();
        this.contactPerson_Type = map.getContactPerson_Type();
        this.organizationDisplayName = map.getOrganizationDisplayName();
        this.organizationName = map.getOrganizationName();
        this.organizationUrl = map.getOrganizationUrl();
        this.idps = map.getIdps();
        this.idpMetadataUrl = map.getIdpMetadataUrl();
        this.spidAttributes = map.getSpidAttributes();
        this.authnContext = map.getAuthnContext();
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
