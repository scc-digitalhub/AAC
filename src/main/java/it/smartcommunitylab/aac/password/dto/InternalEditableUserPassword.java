package it.smartcommunitylab.aac.password.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractEditableUserCredentials;
import it.smartcommunitylab.aac.repository.JsonSchemaIgnore;

@Valid
@JsonInclude(Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalEditableUserPassword extends AbstractEditableUserCredentials {
    private static final long serialVersionUID = SystemKeys.AAC_INTERNAL_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_CREDENTIALS + SystemKeys.ID_SEPARATOR
            + SystemKeys.AUTHORITY_PASSWORD;

    private static final JsonNode schema;
    static {
        schema = generator.generateSchema(InternalEditableUserPassword.class);
    }

    private String credentialsId;

    @NotBlank
    @JsonSchemaIgnore
    private String username;

    @Schema(name = "password", title = "field.password", description = "description.password", format = "password")
    @NotBlank
    private String password;

    @NotBlank
    private String verifyPassword;

    private String curPassword;

    public InternalEditableUserPassword() {
        super(SystemKeys.AUTHORITY_PASSWORD, null, null);
    }

    public InternalEditableUserPassword(String provider, String uuid) {
        super(SystemKeys.AUTHORITY_PASSWORD, provider, uuid);
    }

    public InternalEditableUserPassword(String provider, String realm, String userId, String uuid) {
        super(SystemKeys.AUTHORITY_PASSWORD, provider, uuid);
        setRealm(realm);
        setUserId(userId);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getUsername() {
        return username;
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

    public String getVerifyPassword() {
        return verifyPassword;
    }

    public void setVerifyPassword(String verifyPassword) {
        this.verifyPassword = verifyPassword;
    }

    public String getCurPassword() {
        return curPassword;
    }

    public void setCurPassword(String curPassword) {
        this.curPassword = curPassword;
    }

    @Override
    public JsonNode getSchema() {
        return schema;
    }
}
