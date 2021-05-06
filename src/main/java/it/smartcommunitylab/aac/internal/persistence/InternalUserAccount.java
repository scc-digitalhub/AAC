package it.smartcommunitylab.aac.internal.persistence;

import java.util.Date;
import java.util.HashMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;

@Entity
@Table(name = "internal_users", uniqueConstraints = @UniqueConstraint(columnNames = { "realm", "username" }))
@EntityListeners(AuditingEntityListener.class)
public class InternalUserAccount implements UserAccount {

    @Id
    @GeneratedValue
    private Long id;

    // entity
    @NotNull
    @Column(name = "subject_id")
    private String subject;

    @Transient
    private String provider;

    private String realm;

    @Transient
    private String userId;

    // login
    @Column(name = "username")
    private String username;
    private String password;

    // attributes
    private String email;
    private String name;
    private String surname;

    private String lang;

    // registration
    private boolean confirmed;
    @Column(name = "confirmation_deadline")
    private Date confirmationDeadline;

    @Column(name = "confirmation_key", unique = true, nullable = true)
    private String confirmationKey;

    @Column(name = "reset_deadline")
    private Date resetDeadline;

    @Column(name = "reset_key", unique = true, nullable = true)
    private String resetKey;

    @Column(name = "change_first_access")
    private Boolean changeOnFirstAccess;

    // audit
    @CreatedDate
    @Column(name = "created_date")
    private Date createDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Date modifiedDate;

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_INTERNAL;
    }

    @Override
    public String getProvider() {
        return provider == null ? SystemKeys.AUTHORITY_INTERNAL : provider;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    public String getUsername() {
        return username;
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

    /*
     * fields
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
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

    public Date getResetDeadline() {
        return resetDeadline;
    }

    public void setResetDeadline(Date resetDeadline) {
        this.resetDeadline = resetDeadline;
    }

    public String getResetKey() {
        return resetKey;
    }

    public void setResetKey(String resetKey) {
        this.resetKey = resetKey;
    }

    public Boolean getChangeOnFirstAccess() {
        return changeOnFirstAccess;
    }

    public void setChangeOnFirstAccess(Boolean changeOnFirstAccess) {
        this.changeOnFirstAccess = changeOnFirstAccess;
    }

    public Boolean isChangeOnFirstAccess() {
        return changeOnFirstAccess != null && changeOnFirstAccess;
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

    public void setProvider(String provider) {
        this.provider = provider;
    }

//    @Override
    public AccountProfile toProfile() {
        AccountProfile profile = new AccountProfile();
        profile.setAuthority(getAuthority());
        profile.setProvider(getProvider());
        profile.setRealm(getRealm());
        profile.setUsername(getUsername());
        profile.setUserId(getUserId());

        profile.setAttributes(new HashMap<>());
        profile.getAttributes().put("username", getUsername());
        profile.getAttributes().put("email", getEmail());
        profile.getAttributes().put("given_name", getName());
        profile.getAttributes().put("family_name", getSurname());
        
        return profile;
    }

    @Override
    public String toString() {
        return "InternalUserAccount [id=" + id + ", subject=" + subject + ", provider=" + provider + ", realm=" + realm
                + ", username=" + username + ", email=" + email + ", name=" + name + ", surname=" + surname + ", lang="
                + lang + ", confirmed=" + confirmed + ", confirmationDeadline=" + confirmationDeadline
                + ", confirmationKey=" + confirmationKey + ", resetDeadline=" + resetDeadline + ", resetKey=" + resetKey
                + ", changeOnFirstAccess=" + changeOnFirstAccess + ", createDate=" + createDate + ", modifiedDate="
                + modifiedDate + "]";
    }

}
