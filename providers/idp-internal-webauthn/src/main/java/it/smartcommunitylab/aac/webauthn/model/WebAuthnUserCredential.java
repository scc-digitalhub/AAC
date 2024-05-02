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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.credentials.base.AbstractUserCredentials;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnUserCredential extends AbstractUserCredentials {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CREDENTIALS + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_WEBAUTHN;

    @NotBlank
    @Column(name = "repository_id", length = 128)
    private String repositoryId;

    // // username (requires an account from the same repository for login)
    // @NotBlank
    // @Column(name = "username", length = 128)
    // private String username;

    @NotBlank
    @Column(name = "user_handle")
    private String userHandle;

    @Column(name = "display_name")
    private String displayName;

    @NotBlank
    @Column(name = "credential_id", length = 128)
    private String credentialId;

    // public key as COSE
    @NotBlank
    @Lob
    @Column(name = "public_key_cose")
    private String publicKeyCose;

    @Column(name = "signature_count")
    private long signatureCount = 0L;

    @Column(name = "transports")
    private String transports;

    @Column(name = "discoverable")
    private Boolean discoverable;

    // credentials status
    @Column(length = 32)
    private String status;

    /*
     * Additional fields
     */
    @Lob
    @Column(name = "attestation_object")
    private String attestationObject;

    @Lob
    @Column(name = "client_data")
    private String clientData;

    // audit
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date")
    private Date createDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_used_date")
    private Date lastUsedDate;

    public WebAuthnUserCredential(String realm, String id) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, null, realm, id);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    protected WebAuthnUserCredential() {
        super();
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return id;
    }

    @Override
    public String getCredentialsId() {
        return id;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    @JsonIgnore
    public String getCredentials() {
        return publicKeyCose;
    }

    @Override
    public boolean isActive() {
        return CredentialsStatus.ACTIVE.getValue().equals(status);
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public boolean isRevoked() {
        return CredentialsStatus.REVOKED.getValue().equals(status);
    }

    // public String getUsername() {
    //     return username;
    // }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    // public void setUsername(String username) {
    //     this.username = username;
    // }

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

    public String getPublicKeyCose() {
        return publicKeyCose;
    }

    public void setPublicKeyCose(String publicKeyCose) {
        this.publicKeyCose = publicKeyCose;
    }

    public long getSignatureCount() {
        return signatureCount;
    }

    public void setSignatureCount(long signatureCount) {
        this.signatureCount = signatureCount;
    }

    public String getTransports() {
        return transports;
    }

    public void setTransports(String transports) {
        this.transports = transports;
    }

    public Boolean getDiscoverable() {
        return discoverable;
    }

    public void setDiscoverable(Boolean discoverable) {
        this.discoverable = discoverable;
    }

    public boolean isDiscoverable() {
        return this.discoverable != null ? this.discoverable.booleanValue() : true;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getLastUsedDate() {
        return lastUsedDate;
    }

    public void setLastUsedDate(Date lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAttestationObject() {
        return attestationObject;
    }

    public void setAttestationObject(String attestationObject) {
        this.attestationObject = attestationObject;
    }

    public String getClientData() {
        return clientData;
    }

    public void setClientData(String clientData) {
        this.clientData = clientData;
    }

    @Override
    public void eraseCredentials() {
        this.publicKeyCose = null;
    }

    @Override
    public String toString() {
        return (
            "WebAuthnUserCredential [id=" +
            id +
            ", repositoryId=" +
            repositoryId +
            ", userId=" +
            userId +
            ", userHandle=" +
            userHandle +
            ", displayName=" +
            displayName +
            ", credentialId=" +
            credentialId +
            ", status=" +
            status +
            ", createDate=" +
            createDate +
            ", lastUsedDate=" +
            lastUsedDate +
            "]"
        );
    }
}
