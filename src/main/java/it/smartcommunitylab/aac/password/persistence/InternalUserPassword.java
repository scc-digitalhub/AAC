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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.model.AbstractUserCredentials;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalUserPassword extends AbstractUserCredentials {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CREDENTIALS + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_PASSWORD;

    // unique uuid
    @Id
    @NotBlank
    @Column(name = "id", length = 128)
    private String id;

    @NotBlank
    @Column(name = "repository_id", length = 128)
    private String repositoryId;

    // account id
    @NotBlank
    @Column(name = "username", length = 128)
    private String username;

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

    public InternalUserPassword() {
        super(SystemKeys.AUTHORITY_PASSWORD, null);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return id;
    }

    @Override
    public String getCredentialsId() {
        return id;
    }

    @Override
    public String getAccountId() {
        return username;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    @JsonIgnore
    public String getCredentials() {
        return password;
    }

    @Override
    public boolean isActive() {
        return CredentialsStatus.ACTIVE.getValue().equals(status);
    }

    public boolean isExpired() {
        return expirationDate == null ? false : expirationDate.before(new Date());
    }

    @Override
    public boolean isRevoked() {
        return CredentialsStatus.REVOKED.getValue().equals(status);
    }

    public boolean isChangeOnFirstAccess() {
        return changeOnFirstAccess != null ? changeOnFirstAccess.booleanValue() : false;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAccountId(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
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

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
        this.resetKey = null;
    }

    @Override
    public String toString() {
        return (
            "InternalUserPassword [id=" +
            id +
            ", repositoryId=" +
            repositoryId +
            ", username=" +
            username +
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
