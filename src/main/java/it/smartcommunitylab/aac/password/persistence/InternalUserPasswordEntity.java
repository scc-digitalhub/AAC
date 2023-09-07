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

package it.smartcommunitylab.aac.password.persistence;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "internal_users_passwords",
    uniqueConstraints = @UniqueConstraint(columnNames = { "repository_id", "reset_key" })
)
@EntityListeners(AuditingEntityListener.class)
public class InternalUserPasswordEntity {

    // unique uuid
    @Id
    @NotBlank
    @Column(name = "id", length = 128)
    private String id;

    @NotBlank
    @Column(name = "repository_id", length = 128)
    private String repositoryId;

    // // username (requires an account from the same repository for login)
    // @NotBlank
    // @Column(name = "username", length = 128)
    // private String username;

    // user id
    @NotNull
    @Column(name = "user_id", length = 128)
    private String userId;

    @NotBlank
    @Column(length = 128)
    private String realm;

    // password hash
    @NotBlank
    @Column(length = 512)
    private String password;

    // credentials status
    @Column(length = 32)
    private String status;

    @Column(name = "expiration_date")
    private Date expirationDate;

    @Column(name = "reset_deadline")
    private Date resetDeadline;

    @Column(name = "reset_key", nullable = true)
    private String resetKey;

    @Column(name = "change_first_access")
    private Boolean changeOnFirstAccess;

    // audit
    @CreatedDate
    @Column(name = "created_date")
    private Date createDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    // public String getUsername() {
    //     return username;
    // }

    // public void setUsername(String username) {
    //     this.username = username;
    // }

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Override
    public String toString() {
        return (
            "InternalUserPassword [id=" +
            id +
            ", repositoryId=" +
            repositoryId +
            ", userId=" +
            userId +
            ", status=" +
            status +
            ", createDate=" +
            createDate +
            ", expirationDate=" +
            expirationDate +
            ", resetDeadline=" +
            resetDeadline +
            ", changeOnFirstAccess=" +
            changeOnFirstAccess +
            "]"
        );
    }
}
