package it.smartcommunitylab.aac.attributes.provider;

import java.io.Serializable;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.util.StringUtils;

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
public class ScriptAttributeProviderConfigMap implements ConfigurableProperties, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    private static final String DEFAULT_FUNCTION_CODE = "function attributeMapping(principal) {\n return {}; \n}";

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    // script code in base64
    private String code;

    public ScriptAttributeProviderConfigMap() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @JsonIgnore
    public String getPlaintext() {
        return StringUtils.hasText(code)
                ? new String(Base64.getDecoder().decode(code.getBytes()))
                : DEFAULT_FUNCTION_CODE;
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
        ScriptAttributeProviderConfigMap map = mapper.convertValue(props, ScriptAttributeProviderConfigMap.class);

        // map all props defined in model
        // we expect code in base64, decode if present
        this.code = map.getCode();

    }

    @JsonIgnore
    public static JsonSchema getConfigurationSchema() throws JsonMappingException {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        return schemaGen.generateSchema(ScriptAttributeProviderConfigMap.class);
    }
}
