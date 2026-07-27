package it.smartcommunitylab.aac.otp.provider;

import java.io.Serializable;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class OtpIdentityProviderConfigMap extends AbstractConfigMap {

    private static final Long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_IDENTITY_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_OTP;

    @Max(30 * 24 * 60 * 60)
    private Integer maxSessionDuration;

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String repositoryId;

    private Boolean displayAsButton;

    private Boolean requireAccountConfirmation;

    /*
     * OTP properties
     */

    @Max(5 * 60)
    private Integer otpValidity;

    @Max(5 * 60)
    private Integer otpCooldown;

    @Min(0)
    @Max(3)
    private Integer otpTryNumber;

    public OtpIdentityProviderConfigMap() {}

    public static Long getSerialversionuid() {
        return serialVersionUID;
    }

    public static String getResourceType() {
        return RESOURCE_TYPE;
    }

    public Integer getMaxSessionDuration() {
        return maxSessionDuration;
    }

    public void setMaxSessionDuration(Integer maxSessionDuration) {
        this.maxSessionDuration = maxSessionDuration;
    }

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

    public Boolean getRequireAccountConfirmation() {
        return requireAccountConfirmation;
    }

    public void setRequireAccountConfirmation(Boolean requireAccountConfirmation) {
        this.requireAccountConfirmation = requireAccountConfirmation;
    }

    public Integer getOtpValidity() {
        return otpValidity;
    }

    public void setOtpValidity(Integer otpValidity) {
        this.otpValidity = otpValidity;
    }

    public Integer getOtpCooldown() {
        return otpCooldown;
    }

    public void setOtpCooldown(Integer otpCooldown) {
        this.otpCooldown = otpCooldown;
    }

    public Integer getOtpTryNumber() {
        return otpTryNumber;
    }

    public void setOtpTryNumber(Integer otpTryNumber) {
        this.otpTryNumber = otpTryNumber;
    }

    @JsonIgnore
    public void setConfiguration(OtpIdentityProviderConfigMap config) {
        this.maxSessionDuration = config.getMaxSessionDuration();
        this.repositoryId = config.getRepositoryId();
        this.displayAsButton = config.getDisplayAsButton();
        this.requireAccountConfirmation = config.getRequireAccountConfirmation();
        this.otpValidity = config.getOtpValidity();
        this.otpCooldown = config.getOtpCooldown();
        this.otpTryNumber = config.getOtpTryNumber();
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper for local
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        OtpIdentityProviderConfigMap map = mapper.convertValue(props, OtpIdentityProviderConfigMap.class);

        setConfiguration(map);
    }

    @Override
    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(OtpIdentityProviderConfigMap.class);
    }
}
