package it.smartcommunitylab.aac.webauthn.persistence;

import java.io.Serializable;
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

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.CredentialsContainer;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractUserCredentials;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.internal.model.CredentialsType;

@Entity
@Table(name = "internal_users_webauthn_credentials", uniqueConstraints = @UniqueConstraint(columnNames = {
        "provider_id", "user_handle", "credential_id" }))
@EntityListeners(AuditingEntityListener.class)
public class WebAuthnUserCredential extends AbstractUserCredentials
        implements CredentialsContainer, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    // id is internal
    @Id
    @NotBlank
    @Column(name = "id", length = 128)
    private String id;

    @NotBlank
    @Column(name = "provider_id", length = 128)
    private String provider;

    // account id (with the same provider)
    @NotBlank
    @Column(name = "username", length = 128)
    private String username;

    @NotBlank
    @Column(name = "user_handle")
    private String userHandle;

    /**
     * A custom name the user can associate to this credential It can be used, for
     * example, to help distinguishing authenticators.
     * 
     * E.g., a credential may be called 'Yubico 5c' to make it obvious in the web
     * interface that it is relative to that authenticator
     */
    @Column(name = "display_name")
    private String displayName;

    /**
     * Public key of this credential
     */
    @NotBlank
    @Column(name = "credential_id", length = 128)
    private String credentialId;

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

    @Column(name = "status", length = 32)
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

    /*
     * Audit
     */

    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date")
    private Date createDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_used_date")
    private Date lastUsedDate;

    private transient String realm;

    public WebAuthnUserCredential() {
        super(SystemKeys.AUTHORITY_WEBAUTHN, null, null, null);
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public CredentialsType getCredentialsType() {
        return CredentialsType.WEBAUTHN;
    }

    @Override
    public String getUuid() {
        return credentialId;
    }

    @Override
    public String getAccountId() {
        return username;
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

    @Override
    public boolean isChangeOnFirstAccess() {
        return false;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
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

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
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
}
