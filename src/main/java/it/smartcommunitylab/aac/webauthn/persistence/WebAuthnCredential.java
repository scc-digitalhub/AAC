package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.Date;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;

@Embeddable
public class WebAuthnCredential {

    @Id
    private ByteArray credentialId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webauth_acc_id", nullable = false)
    private WebAuthnUserAccount parentAccount;

    /**
     * A custom name the user can associate to this credential
     * It can be used, for example, to help distinguishing authenticators.
     * 
     * E.g., a credential may be called 'Yubico 5c' to make it obvious in the web
     * interface that it is relative to that authenticator
     */
    private String displayName;

    /**
     * Wether the registration ceremony has been completed or it has just started
     */
    private boolean hasCompletedRegistration = false;

    /**
     * Public key of this credential
     */
    private ByteArray publicKeyCose;

    private Long signatureCount = 0L;

    @ElementCollection
    private Set<AuthenticatorTransport> transports;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdOn;
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUsedOn;

    public RegisteredCredential getRegisteredCredential() {
        return RegisteredCredential.builder().credentialId(credentialId).userHandle(parentAccount.getUserHandle())
                .publicKeyCose(publicKeyCose).signatureCount(signatureCount).build();
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
    public void setCredentialId(ByteArray credentialId) {
        this.credentialId = credentialId;
    }

    public ByteArray getCredentialId() {
        return credentialId;
    }

    public void setPublicKeyCose(ByteArray publicKeyCose) {
        this.publicKeyCose = publicKeyCose;
    }

    public ByteArray getPublicKeyCose() {
        return publicKeyCose;
    }

    public Set<AuthenticatorTransport> getTransports() {
        return transports;
    }

    public void setTransports(Set<AuthenticatorTransport> transports) {
        this.transports = transports;
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

    public boolean getHasCompletedRegistration() {
        return hasCompletedRegistration;
    }

    public void setHasCompletedRegistration(boolean hasCompletedRegistration) {
        this.hasCompletedRegistration = hasCompletedRegistration;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "Exception while converting to String:" + e.getMessage();
        }
    }

    public String toJSON() {
        return toString();
    }

    static WebAuthnCredential fromJSON(String s) throws JsonMappingException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(s, WebAuthnCredential.class);
    }

    public WebAuthnUserAccount getParentAccount() {
        return parentAccount;
    }

    public void setParentAccount(WebAuthnUserAccount parentAccount) {
        this.parentAccount = parentAccount;
    }
}
