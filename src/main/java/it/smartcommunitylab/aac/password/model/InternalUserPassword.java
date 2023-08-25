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

package it.smartcommunitylab.aac.password.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.credentials.base.AbstractUserCredentials;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import java.util.Date;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalUserPassword extends AbstractUserCredentials {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CREDENTIALS + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_PASSWORD;

    @NotBlank
    private String repositoryId;

    // username (requires an account from the same repository for login)
    @NotBlank
    private String username;

    // password hash
    @NotBlank
    private String password;

    // credentials status
    private String status;

    private Date expirationDate;

    private Date resetDeadline;
    private String resetKey;

    private Boolean changeOnFirstAccess;

    // audit
    private Date createDate;

    public InternalUserPassword(String realm, String id) {
        super(SystemKeys.AUTHORITY_PASSWORD, null, realm, id);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */

    @SuppressWarnings("unused")
    protected InternalUserPassword() {
        super();
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

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
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
