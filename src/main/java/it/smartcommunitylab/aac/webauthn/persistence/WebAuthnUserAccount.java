package it.smartcommunitylab.aac.webauthn.persistence;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import com.yubico.webauthn.data.ByteArray;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserAccount;

@Entity
@Table(name = "webauthn_users", uniqueConstraints = @UniqueConstraint(columnNames = { "realm", "username" }))
@EntityListeners(AuditingEntityListener.class)
public class WebAuthnUserAccount implements UserAccount {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    @Id
    @GeneratedValue
    private Long id;

    private String username;
    private String displayName;
    private ByteArray userHandle;
    @Embedded
    WebAuthnCredential credential;
    private boolean hasCompletedRegistration = false;

    @NotNull
    @Column(name = "subject_id")
    private String subject;
    private String emailAddress;
    private String realm;
    @Transient
    private String userId;
    @Transient
    private String provider;

    public long getId() {
        return id;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String userName) {
        this.username = userName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ByteArray getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(ByteArray userHandle) {
        this.userHandle = userHandle;
    }

    public WebAuthnCredential getCredential() {
        return credential;
    }

    public void setCredential(WebAuthnCredential credential) {
        this.credential = credential;
    }

    public boolean getHasCompletedRegistration() {
        return hasCompletedRegistration;
    }

    public void setHasCompletedRegistration(boolean hasCompletedRegistration) {
        this.hasCompletedRegistration = hasCompletedRegistration;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public String getUserId() {
        if (userId == null) {
            // use our id at authority level is the internal id
            return String.valueOf(id);
        }

        return userId;
    }

    public void setUserId(String id) {
        userId = id;
    }

    @Override
    public String getProvider() {
        return provider == null ? SystemKeys.AUTHORITY_WEBAUTHN : provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_WEBAUTHN;
    }
}
