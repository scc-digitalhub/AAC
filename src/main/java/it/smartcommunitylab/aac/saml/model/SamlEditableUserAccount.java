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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractEditableAccount;
import it.smartcommunitylab.aac.repository.JsonSchemaIgnore;
import java.util.Date;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Valid
@JsonInclude(Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "username", "givenName", "familyName", "lang" })
public class SamlEditableUserAccount extends AbstractEditableAccount {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_ACCOUNT + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_SAML;

    private static final JsonNode schema;

    static {
        schema = generator.generateSchema(SamlEditableUserAccount.class);
    }

    // properties
    @JsonSchemaIgnore
    private String subjectId;

    @JsonSchemaIgnore
    private String email;

    @JsonSchemaIgnore
    private Date createDate;

    @JsonSchemaIgnore
    private Date modifiedDate;

    @Schema(name = "username", title = "field.username", description = "description.username")
    @NotBlank
    private String username;

    // attributes
    @Schema(name = "name", title = "field.name", description = "description.name")
    @Size(min = 2, max = 70)
    private String name;

    @Schema(name = "surname", title = "field.surname", description = "description.surname")
    @Size(min = 2, max = 70)
    private String surname;

    @Schema(name = "language", title = "field.language", description = "description.language")
    private String lang;

    protected SamlEditableUserAccount() {
        super(SystemKeys.AUTHORITY_OIDC, null, null);
    }

    public SamlEditableUserAccount(String provider, String uuid) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, uuid);
    }

    public SamlEditableUserAccount(String authority, String provider, String uuid) {
        super(authority, provider, uuid);
    }

    public SamlEditableUserAccount(String authority, String provider, String realm, String userId, String uuid) {
        super(authority, provider, uuid);
        setRealm(realm);
        setUserId(userId);
    }

    @Override
    public JsonNode getSchema() {
        return schema;
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String getAccountId() {
        return subjectId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
}
