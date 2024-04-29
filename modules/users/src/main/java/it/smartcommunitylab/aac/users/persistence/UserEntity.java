/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.users.persistence;

import it.smartcommunitylab.aac.model.UserStatus;
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

    @Id
    @NotNull
    @Column(length = 128, unique = true)
    private String uuid;

    @NotNull
    @Column(length = 128)
    private String realm;

    @Column(length = 128)
    private String username;

    @Column(name = "email_address", length = 128)
    private String emailAddress;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "language")
    private String lang;

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

    //TODO remove
    @Column(name = "tos_accepted")
    private Boolean tosAccepted;

    protected UserEntity() {}

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

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
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

    public Boolean getTosAccepted() {
        return tosAccepted;
    }

    public void setTosAccepted(Boolean tosAccepted) {
        this.tosAccepted = tosAccepted;
    }

    public boolean isTosAccepted() {
        return tosAccepted != null ? tosAccepted.booleanValue() : false;
    }

    @Override
    public String toString() {
        return (
            "UserEntity [uuid=" +
            uuid +
            ", realm=" +
            realm +
            ", username=" +
            username +
            ", emailAddress=" +
            emailAddress +
            ", emailVerified=" +
            emailVerified +
            ", status=" +
            status +
            ", expirationDate=" +
            expirationDate +
            ", createDate=" +
            createDate +
            ", modifiedDate=" +
            modifiedDate +
            ", tosAccepted=" +
            tosAccepted +
            "]"
        );
    }
}
