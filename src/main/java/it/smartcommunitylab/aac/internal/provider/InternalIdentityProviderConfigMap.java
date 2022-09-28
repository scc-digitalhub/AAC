package it.smartcommunitylab.aac.internal.provider;

import java.io.Serializable;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigMap;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalIdentityProviderConfigMap extends AbstractConfigMap implements Serializable {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    @Max(3 * 24 * 60 * 60)
    protected Integer maxSessionDuration;

    private Boolean enableRegistration;
    private Boolean enableDelete;
    private Boolean enableUpdate;

    private Boolean confirmationRequired;

    @Max(3 * 24 * 60 * 60)
    private Integer confirmationValidity;

    public InternalIdentityProviderConfigMap() {
    }

    public Integer getMaxSessionDuration() {
        return maxSessionDuration;
    }

    public void setMaxSessionDuration(Integer maxSessionDuration) {
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

    @JsonIgnore
    public void setConfiguration(InternalIdentityProviderConfigMap map) {
        this.maxSessionDuration = map.getMaxSessionDuration();

        this.enableRegistration = map.getEnableRegistration();
        this.enableDelete = map.getEnableDelete();
        this.enableUpdate = map.getEnableUpdate();

        this.confirmationRequired = map.getConfirmationRequired();
        this.confirmationValidity = map.getConfirmationValidity();
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        InternalIdentityProviderConfigMap map = mapper.convertValue(props, InternalIdentityProviderConfigMap.class);

        setConfiguration(map);
    }

    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(InternalIdentityProviderConfigMap.class);
    }
}
