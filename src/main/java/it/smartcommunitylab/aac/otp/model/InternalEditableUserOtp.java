package it.smartcommunitylab.aac.otp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.credentials.base.AbstractEditableUserCredentials;
import it.smartcommunitylab.aac.repository.JsonSchemaIgnore;
import javax.validation.Valid;

@Valid
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalEditableUserOtp extends AbstractEditableUserCredentials {

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CREDENTIALS + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_OTP;

    @JsonSchemaIgnore
    private String credentialsId;

    @JsonSchemaIgnore
    private String userId;

    @JsonSchemaIgnore
    private Long createDate;

    @JsonSchemaIgnore
    private Long modifiedDate;

    @JsonSchemaIgnore
    private Long expireDate;

    public InternalEditableUserOtp(String realm, String id) {
        super(SystemKeys.AUTHORITY_OTP, null, realm, id);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public JsonNode getSchema() {
        return null;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Long createDate) {
        this.createDate = createDate;
    }

    public Long getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Long modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public Long getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Long expireDate) {
        this.expireDate = expireDate;
    }
}