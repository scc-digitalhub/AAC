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

package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "webauthn_credentials",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "repository_id", "user_handle", "credential_id" }),
        @UniqueConstraint(columnNames = { "repository_id", "username", "user_handle" }),
    }
)
@EntityListeners(AuditingEntityListener.class)
public class WebAuthnUserCredentialEntity {

    // id is internal
    // unique uuid
    @Id
    @NotBlank
    @Column(name = "id", length = 128)
    private String id;

    @NotBlank
    @Column(name = "repository_id", length = 128)
    private String repositoryId;

    // username (requires an account from the same repository for login)
    @NotBlank
    @Column(name = "username", length = 128)
    private String username;

    // user id
    @NotNull
    @Column(name = "user_id", length = 128)
    private String userId;

    @NotBlank
    @Column(length = 128)
    private String realm;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
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

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    @Override
    public String toString() {
        return (
            "WebAuthnUserCredential [id=" +
            id +
            ", repositoryId=" +
            repositoryId +
            ", username=" +
            username +
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
