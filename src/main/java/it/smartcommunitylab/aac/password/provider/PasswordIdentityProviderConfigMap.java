package it.smartcommunitylab.aac.password.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigMap;
import java.io.Serializable;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasswordIdentityProviderConfigMap extends AbstractConfigMap implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_IDENTITY_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_PASSWORD;

    @Max(30 * 24 * 60 * 60)
    private Integer maxSessionDuration;

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String repositoryId;

    private Boolean displayAsButton;

    private Boolean requireAccountConfirmation;

    /*
     * Password properties
     */

    private Boolean enablePasswordReset;

    @Max(3 * 24 * 60 * 60)
    private Integer passwordResetValidity;

    // password policy, optional
    @Min(1)
    @Max(35)
    private Integer passwordMinLength;

    @Min(1)
    @Max(35)
    private Integer passwordMaxLength;

    @Min(0)
    @Max(10)
    private Integer passwordKeepNumber;

    @Min(1)
    private Integer passwordMaxDays;

    private Boolean passwordRequireAlpha;
    private Boolean passwordRequireUppercaseAlpha;
    private Boolean passwordRequireNumber;
    private Boolean passwordRequireSpecial;
    private Boolean passwordSupportWhitespace;

    public PasswordIdentityProviderConfigMap() {}

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public Boolean getDisplayAsButton() {
        return displayAsButton;
    }

    public void setDisplayAsButton(Boolean displayAsButton) {
        this.displayAsButton = displayAsButton;
    }

    public Integer getMaxSessionDuration() {
        return maxSessionDuration;
    }

    public void setMaxSessionDuration(Integer maxSessionDuration) {
        this.maxSessionDuration = maxSessionDuration;
    }

    public Boolean getRequireAccountConfirmation() {
        return requireAccountConfirmation;
    }

    public void setRequireAccountConfirmation(Boolean requireAccountConfirmation) {
        this.requireAccountConfirmation = requireAccountConfirmation;
    }

    public Boolean getEnablePasswordReset() {
        return enablePasswordReset;
    }

    public void setEnablePasswordReset(Boolean enablePasswordReset) {
        this.enablePasswordReset = enablePasswordReset;
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

    @JsonIgnore
    public void setConfiguration(PasswordIdentityProviderConfigMap map) {
        this.maxSessionDuration = map.getMaxSessionDuration();
        this.repositoryId = map.getRepositoryId();
        this.displayAsButton = map.getDisplayAsButton();
        this.requireAccountConfirmation = map.getRequireAccountConfirmation();

        this.enablePasswordReset = map.getEnablePasswordReset();
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
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper for local
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        PasswordIdentityProviderConfigMap map = mapper.convertValue(props, PasswordIdentityProviderConfigMap.class);

        setConfiguration(map);
    }

    @Override
    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(PasswordIdentityProviderConfigMap.class);
    }
}
