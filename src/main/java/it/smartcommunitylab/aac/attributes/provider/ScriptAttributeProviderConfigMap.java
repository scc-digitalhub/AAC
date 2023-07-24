package it.smartcommunitylab.aac.attributes.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigMap;
import java.io.Serializable;
import java.util.Base64;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.util.StringUtils;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScriptAttributeProviderConfigMap extends AbstractConfigMap {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_ATTRIBUTE_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_SCRIPT;

    private static final String DEFAULT_FUNCTION_CODE = "function attributeMapping(principal) {\n return {}; \n}";

    // script code in base64
    private String code;

    public ScriptAttributeProviderConfigMap() {}

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

    public void setConfiguration(ScriptAttributeProviderConfigMap map) {
        // we expect code in base64, decode if present
        this.code = map.getCode();
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        ScriptAttributeProviderConfigMap map = mapper.convertValue(props, ScriptAttributeProviderConfigMap.class);

        // map all props defined in model
        setConfiguration(map);
    }

    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(ScriptAttributeProviderConfigMap.class);
    }
}
