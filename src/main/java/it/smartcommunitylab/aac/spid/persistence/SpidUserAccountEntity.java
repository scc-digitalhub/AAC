/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.repository.HashMapSerializableConverter;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// TODO: review what class is storing
//  maybe there could be columns more meaningful for spid
@Entity
@IdClass(SpidUserAccountId.class)
@Table(name = "spid_users")
@EntityListeners(AuditingEntityListener.class)
public class SpidUserAccountEntity {

    @Id
    @NotBlank
    @Column(name = "repository_id", length = 128)
    private String repositoryId;

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

    // attributes
    @Column(name = "username", length = 128)
    private String username;

    private String idp;

    @Column(name = "spid_code")
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

    // login
    @Column(length = 32)
    private String status;

    // audit
    @CreatedDate
    @Column(name = "created_date")
    private Date createDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Date modifiedDate;

    @Lob
    @Column(name = "attributes")
    @JsonIgnore
    @Convert(converter = HashMapSerializableConverter.class)
    private Map<String, Serializable> attributes;

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFiscalNumber() {
        return fiscalNumber;
    }

    public void setFiscalNumber(String fiscalNumber) {
        this.fiscalNumber = fiscalNumber;
    }

    public String getSpidCode() {
        return spidCode;
    }

    public void setSpidCode(String spidCode) {
        this.spidCode = spidCode;
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

    public String getIdp() {
        return idp;
    }

    public void setIdp(String idp) {
        this.idp = idp;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIvaCode() {
        return ivaCode;
    }

    public void setIvaCode(String ivaCode) {
        this.ivaCode = ivaCode;
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

    public Map<String, Serializable> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Serializable> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return (
            "SpidUserAccountEntity{" +
            "repositoryId='" +
            repositoryId +
            '\'' +
            ", subjectId='" +
            subjectId +
            '\'' +
            ", uuid='" +
            uuid +
            '\'' +
            ", userId='" +
            userId +
            '\'' +
            ", realm='" +
            realm +
            '\'' +
            ", status='" +
            status +
            '\'' +
            ", username='" +
            username +
            '\'' +
            ", idp='" +
            idp +
            '\'' +
            ", spidCode='" +
            spidCode +
            '\'' +
            ", email='" +
            email +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", surname='" +
            surname +
            '\'' +
            ", phone='" +
            phone +
            '\'' +
            ", fiscalNumber='" +
            fiscalNumber +
            '\'' +
            ", ivaCode='" +
            ivaCode +
            '\'' +
            ", createDate=" +
            createDate +
            ", modifiedDate=" +
            modifiedDate +
            ", attributes=" +
            attributes +
            '}'
        );
    }
}
