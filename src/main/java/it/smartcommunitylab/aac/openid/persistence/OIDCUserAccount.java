package it.smartcommunitylab.aac.openid.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAccount;
import it.smartcommunitylab.aac.model.UserStatus;

@Entity
@IdClass(OIDCUserAccountId.class)
@Table(name = "oidc_users")
@EntityListeners(AuditingEntityListener.class)
public class OIDCUserAccount extends AbstractAccount {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    @Id
    @NotBlank
    @Column(name = "provider_id", length = 128)
    private String provider;

    // subject identifier from external provider
    @Id
    @NotBlank
    @Column(name = "subject", length = 128)
    private String subject;

    // unique uuid (subject entity)
    @NotBlank
    @Column(unique = true, length = 128)
    private String uuid;

    // reference to user
    @NotNull
    @Column(name = "user_id", length = 128)
    private String userId;

    private transient String authority;

    @NotBlank
    @Column(length = 128)
    private String realm;

    // login
    @Column(length = 32)
    private String status;

    // attributes
    @Column(name = "username", length = 128)
    private String username;

    private String issuer;

    private String email;
    @Column(name = "email_verified")
    private Boolean emailVerified;

    private String name;

    @Column(name = "given_name")
    private String givenName;

    @Column(name = "family_name")
    private String familyName;

    @Column(length = 32)
    private String lang;

    @Column(name = "picture_uri")
    private String picture;

    // audit
    @CreatedDate
    @Column(name = "created_date")
    private Date createDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Date modifiedDate;

    public OIDCUserAccount() {
        super(SystemKeys.AUTHORITY_OIDC, null, null);
    }

    public OIDCUserAccount(String authority) {
        super(authority, null, null);
        this.authority = authority;
    }

    @Override
    public String getAuthority() {
        return authority != null ? authority : super.getAuthority();
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public String getId() {
        return subject;
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
        return email;
    }

    @Override
    public boolean isEmailVerified() {
        return (StringUtils.hasText(email) && emailVerified != null) ? emailVerified.booleanValue() : false;
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

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
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

    @Override
    public String toString() {
        return "OIDCUserAccount [provider=" + provider + ", subject=" + subject + ", uuid=" + uuid + ", userId="
                + userId + ", authority=" + authority + ", realm=" + realm + ", status=" + status + ", username="
                + username + ", issuer=" + issuer + ", email=" + email + ", emailVerified=" + emailVerified + ", name="
                + name + ", givenName=" + givenName + ", familyName=" + familyName + ", lang=" + lang + ", picture="
                + picture + ", createDate=" + createDate + ", modifiedDate=" + modifiedDate + "]";
    }

}