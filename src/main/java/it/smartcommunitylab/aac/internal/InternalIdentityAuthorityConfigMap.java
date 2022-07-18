package it.smartcommunitylab.aac.internal;

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
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalIdentityAuthorityConfigMap implements ConfigurableProperties, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    private Boolean enableInvitation;
    private Boolean enableRegistration;
    private Boolean enableDelete;
    private Boolean enableUpdate;

    public InternalIdentityAuthorityConfigMap() {

    }

    public Boolean getEnableInvitation() {
        return enableInvitation;
    }

    public void setEnableInvitation(Boolean enableInvitation) {
        this.enableInvitation = enableInvitation;
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
        InternalIdentityAuthorityConfigMap map = mapper.convertValue(props, InternalIdentityAuthorityConfigMap.class);

        this.enableInvitation = map.getEnableInvitation();
        this.enableRegistration = map.getEnableRegistration();
        this.enableDelete = map.getEnableDelete();
        this.enableUpdate = map.getEnableUpdate();
    }

    @JsonIgnore
    public static JsonSchema getConfigurationSchema() throws JsonMappingException {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        return schemaGen.generateSchema(InternalIdentityAuthorityConfigMap.class);
    }
}
