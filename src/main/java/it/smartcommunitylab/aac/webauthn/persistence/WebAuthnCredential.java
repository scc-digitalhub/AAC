package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;

@Entity
@Table(name = "webauthn_credentials")
public class WebAuthnCredential {

    @Id
    @Column(name = "credential_id")
    private String credentialId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webauth_acc_id", nullable = false)
    private WebAuthnUserAccount parentAccount;

    // TODO: civts, use converters
    /**
     * A custom name the user can associate to this credential
     * It can be used, for example, to help distinguishing authenticators.
     * 
     * E.g., a credential may be called 'Yubico 5c' to make it obvious in the web
     * interface that it is relative to that authenticator
     */
    @Column(name = "display_name")
    private String displayName;

    // TODO: civts, use converters
    /**
     * Public key of this credential
     */
    @Column(name = "public_key_cose")
    private String publicKeyCose;

    @Column(name = "signature_count")
    private Long signatureCount = 0L;

    // TODO: use converters
    @ElementCollection
    @Column(name = "transports")
    private Set<String> transports;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_on")
    private Date createdOn;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_used_on")
    private Date lastUsedOn;

    public RegisteredCredential getRegisteredCredential() {
        return RegisteredCredential.builder().credentialId(ByteArray.fromBase64(getCredentialId()))
                .userHandle(parentAccount.getUserHandle())
                .publicKeyCose(ByteArray.fromBase64(getPublicKeyCose())).signatureCount(signatureCount).build();
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

    // Getters and setters
    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setPublicKeyCose(String publicKeyCose) {
        this.publicKeyCose = publicKeyCose;
    }

    public String getPublicKeyCose() {
        return publicKeyCose;
    }

    public Set<AuthenticatorTransport> getTransports() {
        final Set<AuthenticatorTransport> result = Collections.emptySet();
        for (final String code : transports) {
            result.add(AuthenticatorTransport.valueOf(code));
        }
        return result;
    }

    public void setTransports(Set<AuthenticatorTransport> transports) {
        final Set<String> result = Collections.emptySet();
        for (final AuthenticatorTransport t : transports) {
            if (t == AuthenticatorTransport.USB) {
                result.add("USB");
            } else if (t == AuthenticatorTransport.BLE) {
                result.add("BLE");
            } else if (t == AuthenticatorTransport.NFC) {
                result.add("NFC");
            } else if (t == AuthenticatorTransport.INTERNAL) {
                result.add("INTERNAL");
            } else {
                throw new IllegalArgumentException("Transport not found: " + t);
            }
        }
        this.transports = result;
    }

    public void setSignatureCount(Long signatureCount) {
        this.signatureCount = signatureCount;
    }

    public Long getSignatureCount() {
        return signatureCount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public WebAuthnUserAccount getParentAccount() {
        return parentAccount;
    }

    public void setParentAccount(WebAuthnUserAccount parentAccount) {
        this.parentAccount = parentAccount;
    }
}
