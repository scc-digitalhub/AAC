package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
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

    // TODO: civts, use converter
    @Column(unique = true)
    private String userHandle;
    @OneToMany(mappedBy = "parentAccount", fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<WebAuthnCredential> credentials;

    @NotNull
    @Column(name = "subject_id")
    private String subject;
    private String emailAddress;
    private String realm;
    private String username;

    @Column(name = "provider_id")
    private String provider;

    public Long getId() {
        return id;
    }

    public ByteArray getUserHandle() {
        return ByteArray.fromBase64(userHandle);
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public Set<WebAuthnCredential> getCredentials() {
        return credentials;
    }

    public void setCredentials(Set<WebAuthnCredential> credentials) {
        this.credentials = credentials;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserHandle(ByteArray userHandle) {
        this.userHandle = userHandle.getBase64();
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public String getProvider() {
        return provider == null ? SystemKeys.AUTHORITY_WEBAUTHN : provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_WEBAUTHN;
    }

    @Override
    public String getUserId() {
        if (subject == null) {
            // use our id at authority level is the internal id
            return String.valueOf(id);
        }
        return subject;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
