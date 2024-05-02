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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.base.AbstractEditableAccount;
import it.smartcommunitylab.aac.repository.JsonSchemaIgnore;
import java.util.Date;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Valid
@JsonInclude(Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "username", "email", "name", "surname", "lang" })
public class InternalEditableUserAccount extends AbstractEditableAccount {

    private static final long serialVersionUID = SystemKeys.AAC_INTERNAL_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_ACCOUNT + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_INTERNAL;

    private static final JsonNode schema;

    static {
        schema = generator.generateSchema(InternalEditableUserAccount.class);
    }

    // properties
    //    @Schema(name = "username", title = "field.username", description = "description.username")
    //    @NotBlank
    // NOT editable for now
    @JsonSchemaIgnore
    private String username;

    @JsonSchemaIgnore
    private Date createDate;

    @JsonSchemaIgnore
    private Date modifiedDate;

    // attributes
    @Schema(name = "email", title = "field.email", description = "description.email")
    @NotEmpty
    @Email(message = "{validation.email}")
    private String email;

    @Schema(name = "name", title = "field.name", description = "description.name")
    @Size(min = 2, max = 70)
    private String name;

    @Schema(name = "surname", title = "field.surname", description = "description.surname")
    @Size(min = 2, max = 70)
    private String surname;

    @Schema(name = "language", title = "field.language", description = "description.language")
    private String lang;

    protected InternalEditableUserAccount() {
        super(SystemKeys.AUTHORITY_INTERNAL, null, null, null);
    }

    @Deprecated
    public InternalEditableUserAccount(String provider, String uuid) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, null, uuid);
    }

    @Deprecated
    public InternalEditableUserAccount(String authority, String provider, String uuid) {
        super(authority, provider, null, uuid);
    }

    public InternalEditableUserAccount(String authority, String provider, String realm, String userId, String uuid) {
        super(authority, provider, realm, uuid);
        setUserId(userId);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String getAccountId() {
        return username;
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

    @Override
    public JsonNode getSchema() {
        return schema;
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
