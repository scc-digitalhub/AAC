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

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class UserEntity {

    public static final String ID_PREFIX = "u_";

    // TODO remove numeric id, we should have UUID to avoid locking on create
//    @Id
//    @GeneratedValue
//    private Long id;

    @Id
    @NotNull
    @Column(unique = true)
    private String uuid;

    @NotNull
    private String realm;

    private String username;

    @Column(name = "email_address")
    private String emailAddress;

    /*
     * user status
     */
    // locked means no login
    @Column(name = "is_locked")
    private Boolean locked;

    // blocked means no login + revoke all tokens/sessions
    @Column(name = "is_blocked")
    private Boolean blocked;

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

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
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

    public boolean isLocked() {
        if (this.locked != null) {
            return this.locked.booleanValue();
        }

        return false;
    }

    public boolean isBlocked() {
        if (this.blocked != null) {
            return this.blocked.booleanValue();
        }

        return false;
    }

    public boolean isExpired() {
        if (this.expirationDate != null) {
            Date now = new Date();
            return this.expirationDate.after(now);
        }

        return false;
    }

}
