package it.smartcommunitylab.aac.core.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import it.smartcommunitylab.aac.model.UserStatus;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class UserEntity {

    public static final String ID_PREFIX = "u_";

    @Id
    @NotNull
    @Column(unique = true)
    private String uuid;

    @NotNull
    private String realm;

    private String username;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    /*
     * user status
     */
    @Column(name = "status")
    private String status;

    @Column(name = "expiration_date")
    private Date expirationDate;

    /*
     * audit
     */
    @CreatedDate
    @Column(name = "created_date")
    private Date createDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Date modifiedDate;

    @Column(name = "last_login_date")
    private Date loginDate;

    @Column(name = "last_login_ip")
    private String loginIp;

    @Column(name = "last_login_provider")
    private String loginProvider;

    protected UserEntity() {
    }

    public UserEntity(@NotNull String uuid, @NotNull String realm) {
        super();
        this.uuid = uuid;
        this.realm = realm;
    }

//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isEmailVerified() {
        return emailVerified != null ? emailVerified.booleanValue() : false;
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

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
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

    public Date getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(Date loginDate) {
        this.loginDate = loginDate;
    }

    public String getLoginIp() {
        return loginIp;
    }

    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }

    public String getLoginProvider() {
        return loginProvider;
    }

    public void setLoginProvider(String loginProvider) {
        this.loginProvider = loginProvider;
    }

    public boolean isBlocked() {
        return UserStatus.BLOCKED.getValue().equals(status);
    }

    public boolean isInactive() {
        return UserStatus.INACTIVE.getValue().equals(status);
    }

    public boolean isExpired() {
        if (this.expirationDate != null) {
            Date now = new Date();
            return this.expirationDate.after(now);
        }

        return false;
    }

}
