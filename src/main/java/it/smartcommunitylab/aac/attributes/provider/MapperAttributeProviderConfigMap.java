package it.smartcommunitylab.aac.attributes.provider;

import java.io.Serializable;
import java.util.Map;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.mapper.DefaultAttributesMapper;
import it.smartcommunitylab.aac.core.base.AbstractConfigMap;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapperAttributeProviderConfigMap extends AbstractConfigMap {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_CONFIG + SystemKeys.ID_SEPARATOR
            + SystemKeys.RESOURCE_ATTRIBUTE_PROVIDER + SystemKeys.ID_SEPARATOR
            + SystemKeys.AUTHORITY_MAPPER;

    // mapper type
    // note: all attribute sets will be mapped via the same type
    private String type;

    public MapperAttributeProviderConfigMap() {
        // use default
        type = DefaultAttributesMapper.TYPE;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setConfiguration(MapperAttributeProviderConfigMap map) {
        this.type = map.getType();
    }

    @JsonIgnore
    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        MapperAttributeProviderConfigMap map = mapper.convertValue(props, MapperAttributeProviderConfigMap.class);

        // map all props defined in model
        setConfiguration(map);
    }

    @JsonIgnore
    @Override
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(MapperAttributeProviderConfigMap.class);
    }
}
