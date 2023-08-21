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

package it.smartcommunitylab.aac.saml.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.base.AbstractUserAccount;
import it.smartcommunitylab.aac.model.SubjectStatus;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import org.springframework.util.StringUtils;

public class SamlUserAccount extends AbstractUserAccount {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_ACCOUNT + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_SAML;

    @NotBlank
    private String repositoryId;

    // subject identifier from external provider
    @NotBlank
    private String subjectId;

    // unique uuid (subject entity)
    @NotBlank
    private String uuid;

    // login
    private String status;

    // attributes
    private String username;
    private String issuer;
    private String email;
    private Boolean emailVerified;

    private String name;
    private String surname;

    private String lang;

    // audit

    private Date createDate;

    private Date modifiedDate;

    private Map<String, Serializable> attributes;

    public SamlUserAccount(String provider, String realm, String uuid) {
        super(SystemKeys.AUTHORITY_SAML, provider, realm, uuid);
    }

    public SamlUserAccount(String authority, String provider, String realm, String uuid) {
        super(authority, provider, realm, uuid);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
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
        if (status == null || SubjectStatus.ACTIVE.getValue().equals(status)) {
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

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
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

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Map<String, Serializable> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Serializable> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return (
            "SamlUserAccount [repositoryId=" +
            repositoryId +
            ", subjectId=" +
            subjectId +
            ", uuid=" +
            uuid +
            ", userId=" +
            userId +
            ", realm=" +
            realm +
            ", status=" +
            status +
            ", username=" +
            username +
            ", issuer=" +
            issuer +
            ", email=" +
            email +
            ", emailVerified=" +
            emailVerified +
            ", name=" +
            name +
            ", surname=" +
            surname +
            ", lang=" +
            lang +
            ", createDate=" +
            createDate +
            ", modifiedDate=" +
            modifiedDate +
            "]"
        );
    }
}
