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

package it.smartcommunitylab.aac.internal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.base.AbstractUserAccount;
import it.smartcommunitylab.aac.model.SubjectStatus;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.util.StringUtils;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.ALWAYS)
public class InternalUserAccount extends AbstractUserAccount implements CredentialsContainer {

    private static final long serialVersionUID = SystemKeys.AAC_INTERNAL_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_ACCOUNT + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_INTERNAL;

    @NotBlank
    private String repositoryId;

    // account id
    @NotBlank
    private String username;

    // unique uuid (subject entity)
    @NotBlank
    private String uuid;

    // login
    private String status;

    // attributes
    @Email
    private String email;

    private String name;
    private String surname;
    private String lang;

    // registration
    private boolean confirmed;

    //TODO move confirmation to dedicated model
    private Date confirmationDeadline;
    private String confirmationKey;

    // audit
    private Date createDate;
    private Date modifiedDate;

    public InternalUserAccount(String provider, String realm, String uuid) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm, uuid);
    }

    public InternalUserAccount(String authority, String provider, String realm, String uuid) {
        super(authority, provider, realm, uuid);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    protected InternalUserAccount() {
        super();
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    // @Override
    // public String getAccountId() {
    //     return username;
    // }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getAccountId() {
        //local id is username
        return username;
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
        return StringUtils.hasText(email) && confirmed;
    }

    @Override
    public boolean isLocked() {
        // only active users are *not* locked
        if (status == null || SubjectStatus.ACTIVE.getValue().equals(status)) {
            return false;
        }

        // every other condition locks login
        return true;
    }

    @Override
    public void eraseCredentials() {
        this.confirmationKey = null;
        this.confirmationDeadline = null;
    }

    /*
     * fields
     */

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public void setUsername(String username) {
        this.username = username;
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
    public Map<String, Serializable> getAttributes() {
        // internal account has no additional attributes
        return null;
    }

    @Override
    public void setAttributes(Map<String, Serializable> attributes) {
        // internal account has no additional attributes
    }

    @Override
    public String toString() {
        return (
            "InternalUserAccount [repositoryId=" +
            repositoryId +
            ", username=" +
            username +
            ", uuid=" +
            uuid +
            ", userId=" +
            userId +
            ", realm=" +
            realm +
            ", status=" +
            status +
            ", email=" +
            email +
            ", name=" +
            name +
            ", surname=" +
            surname +
            ", lang=" +
            lang +
            ", confirmed=" +
            confirmed +
            ", createDate=" +
            createDate +
            ", modifiedDate=" +
            modifiedDate +
            "]"
        );
    }
}
