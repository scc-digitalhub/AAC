package it.smartcommunitylab.aac.password.provider;

import java.io.Serializable;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigMap;
import it.smartcommunitylab.aac.internal.model.CredentialsType;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalPasswordIdentityProviderConfigMap extends AbstractConfigMap implements Serializable {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    @Max(30 * 24 * 60 * 60)
    private Integer maxSessionDuration;

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String repositoryId;

    private Boolean displayAsButton;

    protected Boolean confirmationRequired;
    private Boolean enablePasswordReset;

    @Max(3 * 24 * 60 * 60)
    private Integer passwordResetValidity;

    public InternalPasswordIdentityProviderConfigMap() {
    }

    public CredentialsType getCredentialsType() {
        return CredentialsType.PASSWORD;
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

    public Integer getMaxSessionDuration() {
        return maxSessionDuration;
    }

    public void setMaxSessionDuration(Integer maxSessionDuration) {
        this.maxSessionDuration = maxSessionDuration;
    }

    public Boolean getConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(Boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
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

    @JsonIgnore
    public void setConfiguration(InternalPasswordIdentityProviderConfigMap map) {
        this.maxSessionDuration = map.getMaxSessionDuration();
        this.repositoryId = map.getRepositoryId();
        this.displayAsButton = map.getDisplayAsButton();
        this.confirmationRequired = map.getConfirmationRequired();

        this.enablePasswordReset = map.getEnablePasswordReset();
        this.passwordResetValidity = map.getPasswordResetValidity();
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper for local
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        InternalPasswordIdentityProviderConfigMap map = mapper.convertValue(props,
                InternalPasswordIdentityProviderConfigMap.class);

        setConfiguration(map);
    }

    @Override
    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(InternalPasswordIdentityProviderConfigMap.class);
    }
}
