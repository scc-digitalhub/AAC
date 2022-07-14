package it.smartcommunitylab.aac.spid.persistence;

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
@IdClass(SpidUserAccountId.class)
@Table(name = "spid_users", uniqueConstraints = @UniqueConstraint(columnNames = { "realm", "provider_id", "user_id" }))
@EntityListeners(AuditingEntityListener.class)
public class SpidUserAccount extends AbstractAccount {

    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;

    @Id
    @NotBlank
    @Column(name = "provider_id", length = 128)
    private String provider;

    // subject identifier from external provider
    @Id
    @NotBlank
    @Column(name = "subject", length = 128)
    private String subjectId;

    // unique uuid (subject entity)
    @NotBlank
    @Column(unique = true, length = 128)
    private String uuid;

    // reference to user
    @NotNull
    @Column(name = "user_id", length = 128)
    private String userId;

    @NotBlank
    @Column(length = 128)
    private String realm;

    // login
    @Column(length = 32)
    private String status;

    // attributes
    @Column(name = "username", length = 128)
    private String username;

    private String idp;

    @Column(name = "spid_code", length = 128)
    private String spidCode;

    private String email;
    private String name;
    private String surname;

    @Column(name = "mobile_phone")
    private String phone;

    @Column(name = "fiscal_number")
    private String fiscalNumber;

    @Column(name = "iva_code")
    private String ivaCode;

    // audit
    @CreatedDate
    @Column(name = "created_date")
    private Date createDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Date modifiedDate;

    public SpidUserAccount() {
        super(SystemKeys.AUTHORITY_SPID, null, null);
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
    public String getUuid() {
        return uuid;
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
        return StringUtils.hasText(email);
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

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIdp() {
        return idp;
    }

    public void setIdp(String idp) {
        this.idp = idp;
    }

    public String getSpidCode() {
        return spidCode;
    }

    public void setSpidCode(String spidCode) {
        this.spidCode = spidCode;
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

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFiscalNumber() {
        return fiscalNumber;
    }

    public void setFiscalNumber(String fiscalNumber) {
        this.fiscalNumber = fiscalNumber;
    }

    public String getIvaCode() {
        return ivaCode;
    }

    public void setIvaCode(String ivaCode) {
        this.ivaCode = ivaCode;
    }

}
