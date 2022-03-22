package it.smartcommunitylab.aac.saml.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
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
@IdClass(SamlUserAccountId.class)
@Table(name = "saml_users")
@EntityListeners(AuditingEntityListener.class)
public class SamlUserAccount extends AbstractAccount {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    @Id
    @NotBlank
    @Column(name = "provider_id")
    private String provider;

    // subject identifier from external provider
    @Id
    @NotBlank
    @Column(name = "subject")
    private String subjectId;

    // reference to user
    @NotNull
    @Column(name = "user_id")
    private String userId;

    @NotBlank
    private String realm;

    // login
    private String status;

    // attributes
    @Column(name = "username")
    private String username;

    @Column(name = "issuer")
    private String issuer;

    @Column(name = "subject_format")
    private String subjectFormat;

    private String email;
    @Column(name = "email_verified")
    private Boolean emailVerified;

    private String name;

    private String lang;

    // audit
    @CreatedDate
    @Column(name = "created_date")
    private Date createDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Date modifiedDate;

    public SamlUserAccount() {
        super(SystemKeys.AUTHORITY_SAML, null, null);
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public String getId() {
        return subjectId;
    }

    @Override
    public String getUserId() {
        return userId;
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
        return UserStatus.LOCKED.getValue().equals(status);
    }

    /*
     * fields
     */

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
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

    public String getSubjectFormat() {
        return subjectFormat;
    }

    public void setSubjectFormat(String subjectFormat) {
        this.subjectFormat = subjectFormat;
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

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
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

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
