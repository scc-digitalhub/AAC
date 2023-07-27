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

package it.smartcommunitylab.aac.webauthn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractEditableUserCredentials;
import it.smartcommunitylab.aac.repository.JsonSchemaIgnore;
import java.util.Date;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Valid
@JsonInclude(Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnEditableUserCredential extends AbstractEditableUserCredentials {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CREDENTIALS + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_WEBAUTHN;

    private static final JsonNode schema;

    static {
        schema = generator.generateSchema(WebAuthnEditableUserCredential.class);
    }

    private String credentialsId;

    @NotBlank
    @JsonSchemaIgnore
    private String username;

    @JsonSchemaIgnore
    private String userHandle;

    @JsonSchemaIgnore
    private Date createDate;

    @JsonSchemaIgnore
    private Date modifiedDate;

    @JsonSchemaIgnore
    private Date expireDate;

    @JsonSchemaIgnore
    private Date lastUsedDate;

    @NotBlank
    private String displayName;

    // attestation for new registrations
    @JsonSchemaIgnore
    private String key;

    @JsonSchemaIgnore
    private String attestation;

    public WebAuthnEditableUserCredential() {
        super(SystemKeys.AUTHORITY_WEBAUTHN, null, null);
    }

    public WebAuthnEditableUserCredential(String provider, String uuid) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, provider, uuid);
    }

    public WebAuthnEditableUserCredential(String provider, String realm, String userId, String uuid) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, provider, uuid);
        setRealm(realm);
        setUserId(userId);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    public Date getLastUsedDate() {
        return lastUsedDate;
    }

    public void setLastUsedDate(Date lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getAttestation() {
        return attestation;
    }

    public void setAttestation(String attestation) {
        this.attestation = attestation;
    }

    @Override
    public JsonNode getSchema() {
        return schema;
    }
}
