package it.smartcommunitylab.aac.internal.provider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Max;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.internal.model.CredentialsType;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalIdentityProviderConfigMap implements ConfigurableProperties, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected static ObjectMapper mapper = new ObjectMapper();
    protected final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    @Max(3 * 24 * 60 * 60)
    protected Integer maxSessionDuration;

    protected CredentialsType credentialsType;
    protected Boolean isolateData;

    protected Boolean enableRegistration;
    protected Boolean enableDelete;
    protected Boolean enableUpdate;

    protected Boolean confirmationRequired;
    @Max(3 * 24 * 60 * 60)
    protected Integer confirmationValidity;

    public InternalIdentityProviderConfigMap() {
    }

    public Integer getMaxSessionDuration() {
        return maxSessionDuration;
    }

    public void setMaxSessionDuration(Integer maxSessionDuration) {
        this.maxSessionDuration = maxSessionDuration;
    }

    public CredentialsType getCredentialsType() {
        return credentialsType;
    }

    public void setCredentialsType(CredentialsType credentialsType) {
        this.credentialsType = credentialsType;
    }

    public Boolean getIsolateData() {
        return isolateData;
    }

    public void setIsolateData(Boolean isolateData) {
        this.isolateData = isolateData;
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

        this.credentialsType = map.getCredentialsType();
        this.isolateData = map.getIsolateData();

        this.enableRegistration = map.getEnableRegistration();
        this.enableDelete = map.getEnableDelete();
        this.enableUpdate = map.getEnableUpdate();

        this.confirmationRequired = map.getConfirmationRequired();
        this.confirmationValidity = map.getConfirmationValidity();
    }

    @JsonIgnore
    public static JsonSchema getConfigurationSchema() throws JsonMappingException {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        return schemaGen.generateSchema(InternalIdentityProviderConfigMap.class);
    }
}
