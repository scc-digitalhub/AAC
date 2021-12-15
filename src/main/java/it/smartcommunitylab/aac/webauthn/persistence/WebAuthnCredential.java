package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;

@Embeddable
public class WebAuthnCredential {

    @Column(unique = true)
    private ByteArray credentialId;

    private ByteArray userHandle;

    /**
     * A custom name the user can associate to this credential
     * It can be used, for example, to help distinguishing authenticators.
     * 
     * E.g., a credential may be called 'Yubico 5c' to make it obvious in the web
     * interface that it is relative to that authenticator
     */
    private String nickname;

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
        return RegisteredCredential.builder().credentialId(credentialId).userHandle(userHandle)
                .publicKeyCose(publicKeyCose).signatureCount(signatureCount).build();
    }

    // Getters and setters
    public void setCredentialId(ByteArray credentialId) {
        this.credentialId = credentialId;
    }

    public ByteArray getCredentialId() {
        return credentialId;
    }

    public void setUserHandle(ByteArray userHandle) {
        this.userHandle = userHandle;
    }

    public ByteArray getUserHandle() {
        return userHandle;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public void setPublicKeyCose(ByteArray publicKeyCose) {
        this.publicKeyCose = publicKeyCose;
    }

    public ByteArray getPublicKeyCose() {
        return publicKeyCose;
    }

    public void setSignatureCount(Long signatureCount) {
        this.signatureCount = signatureCount;
    }

    public Long getSignatureCount() {
        return signatureCount;
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
}
