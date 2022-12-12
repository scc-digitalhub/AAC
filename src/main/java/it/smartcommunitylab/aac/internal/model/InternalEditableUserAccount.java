package it.smartcommunitylab.aac.internal.model;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractEditableAccount;

@Valid
@JsonInclude(Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "username", "email", "name", "surname", "lang" })
public class InternalEditableUserAccount extends AbstractEditableAccount {
    private static final long serialVersionUID = SystemKeys.AAC_INTERNAL_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_ACCOUNT + SystemKeys.ID_SEPARATOR
            + SystemKeys.AUTHORITY_INTERNAL;

    private static final JsonNode schema;
    static {
        schema = generator.generateSchema(InternalEditableUserAccount.class);
    }

    @Schema(name = "username", title = "field.username", description = "description.username")
    @NotBlank
    private String username;

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
        super(SystemKeys.AUTHORITY_INTERNAL, null, null);
    }

    public InternalEditableUserAccount(String provider, String uuid) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, uuid);
    }

    public InternalEditableUserAccount(String provider, String realm, String userId, String uuid) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, uuid);
        setRealm(realm);
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

}
