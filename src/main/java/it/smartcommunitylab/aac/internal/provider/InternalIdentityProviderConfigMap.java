package it.smartcommunitylab.aac.internal.provider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalIdentityProviderConfigMap implements ConfigurableProperties, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    private int maxSessionDuration = 24 * 60 * 60; // 24h

    private Boolean enableRegistration;
    private Boolean enableDelete;
    private Boolean enableUpdate;

    private Boolean enablePasswordReset;
    private Boolean enablePasswordSet;
    private Integer passwordResetValidity;

    private Boolean confirmationRequired;
    private Integer confirmationValidity;

    // password policy, optional
    private Integer passwordMinLength;
    private Integer passwordMaxLength;
    private Boolean passwordRequireAlpha;
    private Boolean passwordRequireNumber;
    private Boolean passwordRequireSpecial;
    private Boolean passwordSupportWhitespace;

    public InternalIdentityProviderConfigMap() {

    }

    public int getMaxSessionDuration() {
        return maxSessionDuration;
    }

    public void setMaxSessionDuration(int maxSessionDuration) {
        this.maxSessionDuration = maxSessionDuration;
    }

    public Boolean getEnableRegistration() {
        return enableRegistration;
    }

    public void setEnableRegistration(Boolean enableRegistration) {
        this.enableRegistration = enableRegistration;
    }

    public Boolean getEnableDelete() {
        return enableDelete;
    }

    public void setEnableDelete(Boolean enableDelete) {
        this.enableDelete = enableDelete;
    }

    public Boolean getEnableUpdate() {
        return enableUpdate;
    }

    public void setEnableUpdate(Boolean enableUpdate) {
        this.enableUpdate = enableUpdate;
    }

    public Boolean getEnablePasswordReset() {
        return enablePasswordReset;
    }

    public void setEnablePasswordReset(Boolean enablePasswordReset) {
        this.enablePasswordReset = enablePasswordReset;
    }

    public Boolean getEnablePasswordSet() {
        return enablePasswordSet;
    }

    public void setEnablePasswordSet(Boolean enablePasswordSet) {
        this.enablePasswordSet = enablePasswordSet;
    }

    public Integer getPasswordResetValidity() {
        return passwordResetValidity;
    }

    public void setPasswordResetValidity(Integer passwordResetValidity) {
        this.passwordResetValidity = passwordResetValidity;
    }

    public Boolean getConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(Boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }

    public Integer getConfirmationValidity() {
        return confirmationValidity;
    }

    public void setConfirmationValidity(Integer confirmationValidity) {
        this.confirmationValidity = confirmationValidity;
    }

    public Integer getPasswordMinLength() {
        return passwordMinLength;
    }

    public void setPasswordMinLength(Integer passwordMinLength) {
        this.passwordMinLength = passwordMinLength;
    }

    public Integer getPasswordMaxLength() {
        return passwordMaxLength;
    }

    public void setPasswordMaxLength(Integer passwordMaxLength) {
        this.passwordMaxLength = passwordMaxLength;
    }

    public Boolean getPasswordRequireAlpha() {
        return passwordRequireAlpha;
    }

    public void setPasswordRequireAlpha(Boolean passwordRequireAlpha) {
        this.passwordRequireAlpha = passwordRequireAlpha;
    }

    public Boolean getPasswordRequireNumber() {
        return passwordRequireNumber;
    }

    public void setPasswordRequireNumber(Boolean passwordRequireNumber) {
        this.passwordRequireNumber = passwordRequireNumber;
    }

    public Boolean getPasswordRequireSpecial() {
        return passwordRequireSpecial;
    }

    public void setPasswordRequireSpecial(Boolean passwordRequireSpecial) {
        this.passwordRequireSpecial = passwordRequireSpecial;
    }

    public Boolean getPasswordSupportWhitespace() {
        return passwordSupportWhitespace;
    }

    public void setPasswordSupportWhitespace(Boolean passwordSupportWhitespace) {
        this.passwordSupportWhitespace = passwordSupportWhitespace;
    }

    @Override
    @JsonIgnore
    public Map<String, Serializable> getConfiguration() {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(this, typeRef);
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        InternalIdentityProviderConfigMap map = mapper.convertValue(props, InternalIdentityProviderConfigMap.class);

        this.maxSessionDuration = map.getMaxSessionDuration();

        this.confirmationRequired = map.getConfirmationRequired();
        this.enableRegistration = map.getEnableRegistration();
        this.enableDelete = map.getEnableDelete();
        this.enableUpdate = map.getEnableUpdate();

        this.enablePasswordReset = map.getEnablePasswordReset();
        this.enablePasswordSet = map.getEnablePasswordSet();
        this.passwordResetValidity = map.getPasswordResetValidity();

        this.confirmationRequired = map.getConfirmationRequired();
        this.confirmationValidity = map.getConfirmationValidity();

        // password policy, optional
        this.passwordMinLength = map.getPasswordMinLength();
        this.passwordMaxLength = map.getPasswordMaxLength();
        this.passwordRequireAlpha = map.getPasswordRequireAlpha();
        this.passwordRequireNumber = map.getPasswordRequireNumber();
        this.passwordRequireSpecial = map.getPasswordRequireSpecial();
        this.passwordSupportWhitespace = map.getPasswordSupportWhitespace();

    }

    @JsonIgnore
    public static JsonSchema getConfigurationSchema() throws JsonMappingException {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        return schemaGen.generateSchema(InternalIdentityProviderConfigMap.class);
    }
}
