package it.smartcommunitylab.aac.webauthn.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserAccount;

@Entity
@Table(name = "webauthn_users", uniqueConstraints = @UniqueConstraint(columnNames = { "provider_id", "username" }))
@EntityListeners(AuditingEntityListener.class)
public class WebAuthnUserAccount implements UserAccount {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
 
    @Id
    @Column(name = "user_handle")
    private String userHandle;

    @NotNull
    @Column(name = "subject_id")
    private String subject;
    @Column(name = "email_address")
    private String emailAddress;
    @Column(name = "realm")
    private String realm;
    @Column(name = "username")
    private String username;

    @Column(name = "provider_id")
    private String provider;
 

    public String getUserHandle() {
        return userHandle;
    }

    public String getEmailAddress() {
        return emailAddress;
    }
  
    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public String getProvider() {
        return provider;
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
            return userHandle;
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
