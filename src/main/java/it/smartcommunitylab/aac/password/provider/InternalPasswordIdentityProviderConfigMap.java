package it.smartcommunitylab.aac.password.provider;

import java.io.Serializable;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.model.CredentialsType;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalPasswordIdentityProviderConfigMap extends InternalIdentityProviderConfigMap {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    /*
     * Password properties
     */
    private Boolean displayAsButton;

    private Boolean enablePasswordReset;
    private Boolean enablePasswordSet;

    @Max(3 * 24 * 60 * 60)
    private Integer passwordResetValidity;

    // password policy, optional
    @Min(1)
    @Max(35)
    private Integer passwordMinLength;
    @Min(1)
    @Max(35)
    private Integer passwordMaxLength;
    private Boolean passwordRequireAlpha;
    private Boolean passwordRequireUppercaseAlpha;
    private Boolean passwordRequireNumber;
    private Boolean passwordRequireSpecial;
    private Boolean passwordSupportWhitespace;
    private Integer passwordKeepNumber;
    private Integer passwordMaxDays;

    public InternalPasswordIdentityProviderConfigMap() {
        super();
        this.credentialsType = CredentialsType.PASSWORD;
    }

    @Override
    public CredentialsType getCredentialsType() {
        return CredentialsType.PASSWORD;
    }

    public Boolean getDisplayAsButton() {
        return displayAsButton;
    }

    public void setDisplayAsButton(Boolean displayAsButton) {
        this.displayAsButton = displayAsButton;
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

    public Boolean getPasswordRequireUppercaseAlpha() {
        return passwordRequireUppercaseAlpha;
    }

    public void setPasswordRequireUppercaseAlpha(Boolean passwordRequireUppercaseAlpha) {
        this.passwordRequireUppercaseAlpha = passwordRequireUppercaseAlpha;
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

    public Integer getPasswordKeepNumber() {
        return passwordKeepNumber;
    }

    public void setPasswordKeepNumber(Integer passwordKeepNumber) {
        this.passwordKeepNumber = passwordKeepNumber;
    }

    public Integer getPasswordMaxDays() {
        return passwordMaxDays;
    }

    public void setPasswordMaxDays(Integer passwordMaxDays) {
        this.passwordMaxDays = passwordMaxDays;
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
        // set base via super
        super.setConfiguration(props);

        // use mapper for local
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        InternalPasswordIdentityProviderConfigMap map = mapper.convertValue(props, InternalPasswordIdentityProviderConfigMap.class);
        this.credentialsType = CredentialsType.PASSWORD;

        this.enablePasswordReset = map.getEnablePasswordReset();
        this.enablePasswordSet = map.getEnablePasswordSet();
        this.passwordResetValidity = map.getPasswordResetValidity();

        // password policy, optional
        this.passwordMinLength = map.getPasswordMinLength();
        this.passwordMaxLength = map.getPasswordMaxLength();
        this.passwordRequireAlpha = map.getPasswordRequireAlpha();
        this.passwordRequireUppercaseAlpha = map.getPasswordRequireUppercaseAlpha();
        this.passwordRequireNumber = map.getPasswordRequireNumber();
        this.passwordRequireSpecial = map.getPasswordRequireSpecial();
        this.passwordSupportWhitespace = map.getPasswordSupportWhitespace();
        this.passwordKeepNumber = map.getPasswordKeepNumber();
        this.passwordMaxDays = map.getPasswordMaxDays();

        this.displayAsButton = map.getDisplayAsButton();
    }

    @JsonIgnore
    public static JsonSchema getConfigurationSchema() throws JsonMappingException {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        return schemaGen.generateSchema(InternalPasswordIdentityProviderConfigMap.class);
    }
}
