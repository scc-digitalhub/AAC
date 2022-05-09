package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAccount;
import it.smartcommunitylab.aac.model.UserStatus;

@Entity
@IdClass(WebAuthnUserAccountId.class)
@Table(name = "webauthn_users", uniqueConstraints = @UniqueConstraint(columnNames = { "provider_id", "user_handle" }))
@EntityListeners(AuditingEntityListener.class)
public class WebAuthnUserAccount extends AbstractAccount implements CredentialsContainer {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    // account id
    @Id
    @NotBlank
    @Column(name = "provider_id")
    private String provider;

    @Id
    @NotBlank
    @Column(name = "username")
    private String username;

    // user handle as per webauthn spec is a unique identifier for a user
    @Column(name = "user_handle")
    private String userHandle;

    // unique uuid (subject entity)
    @NotBlank
    @Column(unique = true)
    private String uuid;

    // user id
    @NotNull
    @Column(name = "user_id")
    private String userId;

    @NotBlank
    private String realm;

    // login
    private String status;

    // attributes
    @Email
    @Column(name = "email_address")
    private String emailAddress;
    private String name;
    private String surname;
    private String lang;

    // confirmation
    private boolean confirmed;
    @Column(name = "confirmation_deadline")
    private Date confirmationDeadline;

    @Column(name = "confirmation_key", unique = true, nullable = true)
    private String confirmationKey;

    // audit
    @CreatedDate
    @Column(name = "created_date")
    private Date createDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Date modifiedDate;

    public WebAuthnUserAccount() {
        super(SystemKeys.AUTHORITY_WEBAUTHN, null, null);
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_WEBAUTHN;
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public String getId() {
        return username;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getEmailAddress() {
        return emailAddress;
    }

    @Override
    public boolean isEmailVerified() {
        return StringUtils.hasText(emailAddress) && confirmed;
    }

    @Override
    public boolean isLocked() {
        // only active users are *not* locked
        if (status == null || UserStatus.ACTIVE.getValue().equals(status)) {
            return false;
        }

        // every other condition locks login
        return true;
    }

    /*
     * fields
     */

    public String getRealm() {
        return realm;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public Date getConfirmationDeadline() {
        return confirmationDeadline;
    }

    public void setConfirmationDeadline(Date confirmationDeadline) {
        this.confirmationDeadline = confirmationDeadline;
    }

    public String getConfirmationKey() {
        return confirmationKey;
    }

    public void setConfirmationKey(String confirmationKey) {
        this.confirmationKey = confirmationKey;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    @Override
    public void eraseCredentials() {
        this.confirmationKey = null;
    }
}
