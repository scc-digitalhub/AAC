package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotBlank;

@Entity
@IdClass(WebAuthnCredentialId.class)
@Table(name = "webauthn_credentials")
public class WebAuthnCredential {

    // credential id
    @Id
    @NotBlank
    @Column(name = "provider_id")
    private String provider;

    @Id
    @NotBlank
    @Column(name = "credential_id")
    private String credentialId;

    @Column(name = "user_handle", nullable = false)
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
    @Column(name = "public_key_cose")
    private String publicKeyCose;

    @Column(name = "signature_count")
    private long signatureCount = 0L;

    /**
     * Comma-separated list of the transports
     */
    @Column(name = "transports")
    private String transports;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_on")
    private Date createdOn;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_used_on")
    private Date lastUsedOn;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public Date getLastUsedOn() {
        return lastUsedOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public void setLastUsedOn(Date lastUsedOn) {
        this.lastUsedOn = lastUsedOn;
    }

    public void setPublicKeyCose(String publicKeyCose) {
        this.publicKeyCose = publicKeyCose;
    }

    public String getPublicKeyCose() {
        return publicKeyCose;
    }

    public String getTransports() {
        return transports;
    }

    public void setTransports(String transports) {
        this.transports = transports;
    }

    public void setSignatureCount(long signatureCount) {
        this.signatureCount = signatureCount;
    }

    public long getSignatureCount() {
        return signatureCount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }
}
