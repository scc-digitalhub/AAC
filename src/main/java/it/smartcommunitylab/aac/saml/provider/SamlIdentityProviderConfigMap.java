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

package it.smartcommunitylab.aac.saml.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import jakarta.validation.Valid;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class SamlIdentityProviderConfigMap extends AbstractConfigMap implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_IDENTITY_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_SAML;

    private String entityId;

    private String signingKey;
    private String signingCertificate;
    private String cryptKey;
    private String cryptCertificate;

    // ap autoconfiguration
    private String idpMetadataUrl;

    // ap manual configuration (only if not metadata)
    private String idpEntityId;
    private String webSsoUrl;
    private String webLogoutUrl;
    private Boolean signAuthNRequest;
    private String verificationCertificate;
    private String ssoServiceBinding;

    // advanced
    private String nameIDFormat;
    private Boolean nameIDAllowCreate;
    private Boolean forceAuthn;
    private Boolean isPassive;
    private Set<String> authnContextClasses;
    private String authnContextComparison;
    private String userNameAttributeName;

    private Boolean trustEmailAddress;
    private Boolean alwaysTrustEmailAddress;
    private Boolean requireEmailAddress;

    public SamlIdentityProviderConfigMap() {}

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

    public String getCryptKey() {
        return cryptKey;
    }

    public void setCryptKey(String cryptKey) {
        this.cryptKey = cryptKey;
    }

    public String getCryptCertificate() {
        return cryptCertificate;
    }

    public void setCryptCertificate(String cryptCertificate) {
        this.cryptCertificate = cryptCertificate;
    }

    public String getIdpMetadataUrl() {
        return idpMetadataUrl;
    }

    public void setIdpMetadataUrl(String idpMetadataUrl) {
        this.idpMetadataUrl = idpMetadataUrl;
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }

    public void setIdpEntityId(String idpEntityId) {
        this.idpEntityId = idpEntityId;
    }

    public String getWebSsoUrl() {
        return webSsoUrl;
    }

    public void setWebSsoUrl(String webSsoUrl) {
        this.webSsoUrl = webSsoUrl;
    }

    public String getWebLogoutUrl() {
        return webLogoutUrl;
    }

    public void setWebLogoutUrl(String webLogoutUrl) {
        this.webLogoutUrl = webLogoutUrl;
    }

    public Boolean getSignAuthNRequest() {
        return signAuthNRequest;
    }

    public void setSignAuthNRequest(Boolean signAuthNRequest) {
        this.signAuthNRequest = signAuthNRequest;
    }

    public String getVerificationCertificate() {
        return verificationCertificate;
    }

    public void setVerificationCertificate(String verificationCertificate) {
        this.verificationCertificate = verificationCertificate;
    }

    public String getSsoServiceBinding() {
        return ssoServiceBinding;
    }

    public void setSsoServiceBinding(String ssoServiceBinding) {
        this.ssoServiceBinding = ssoServiceBinding;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getNameIDFormat() {
        return nameIDFormat;
    }

    public void setNameIDFormat(String nameIDFormat) {
        this.nameIDFormat = nameIDFormat;
    }

    public Boolean getNameIDAllowCreate() {
        return nameIDAllowCreate;
    }

    public void setNameIDAllowCreate(Boolean nameIDAllowCreate) {
        this.nameIDAllowCreate = nameIDAllowCreate;
    }

    public Set<String> getAuthnContextClasses() {
        return authnContextClasses;
    }

    public void setAuthnContextClasses(Set<String> authnContextClasses) {
        this.authnContextClasses = authnContextClasses;
    }

    public String getAuthnContextComparison() {
        return authnContextComparison;
    }

    public void setAuthnContextComparison(String authnContextComparison) {
        this.authnContextComparison = authnContextComparison;
    }

    public Boolean getForceAuthn() {
        return forceAuthn;
    }

    public void setForceAuthn(Boolean forceAuthn) {
        this.forceAuthn = forceAuthn;
    }

    public Boolean getIsPassive() {
        return isPassive;
    }

    public void setIsPassive(Boolean isPassive) {
        this.isPassive = isPassive;
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

    @JsonIgnore
    public void setConfiguration(SamlIdentityProviderConfigMap map) {
        this.signingKey = map.getSigningKey();
        this.signingCertificate = map.getSigningCertificate();
        this.cryptKey = map.getCryptKey();
        this.cryptCertificate = map.getCryptCertificate();

        this.idpMetadataUrl = map.getIdpMetadataUrl();

        this.idpEntityId = map.getIdpEntityId();
        this.webSsoUrl = map.getWebSsoUrl();
        this.webLogoutUrl = map.getWebLogoutUrl();
        this.signAuthNRequest = map.getSignAuthNRequest();
        this.verificationCertificate = map.getVerificationCertificate();
        this.ssoServiceBinding = map.getSsoServiceBinding();

        this.nameIDFormat = map.getNameIDFormat();
        this.nameIDAllowCreate = map.getNameIDAllowCreate();
        this.forceAuthn = map.getForceAuthn();
        this.isPassive = map.getIsPassive();
        this.authnContextClasses = map.getAuthnContextClasses();
        this.authnContextComparison = map.getAuthnContextComparison();

        this.userNameAttributeName = map.getUserNameAttributeName();
        this.trustEmailAddress = map.getTrustEmailAddress();
        this.alwaysTrustEmailAddress = map.getAlwaysTrustEmailAddress();
        this.requireEmailAddress = map.getRequireEmailAddress();

        this.entityId = map.getEntityId();
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        SamlIdentityProviderConfigMap map = mapper.convertValue(props, SamlIdentityProviderConfigMap.class);

        setConfiguration(map);
    }

    @Override
    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(SamlIdentityProviderConfigMap.class);
    }
}
