package it.smartcommunitylab.aac.internal.provider;

import java.io.Serializable;
import java.util.Map;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigMap;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalAttributeProviderConfigMap extends AbstractConfigMap {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_CONFIG + SystemKeys.ID_SEPARATOR
            + SystemKeys.RESOURCE_ATTRIBUTE_PROVIDER + SystemKeys.ID_SEPARATOR
            + SystemKeys.AUTHORITY_INTERNAL;

    private Boolean usermode;

    public InternalAttributeProviderConfigMap() {
    }

    public Boolean getUsermode() {
        return usermode;
    }

    public void setUsermode(Boolean usermode) {
        this.usermode = usermode;
    }

    public void setConfiguration(InternalAttributeProviderConfigMap map) {
        this.usermode = map.getUsermode();
    }

    @JsonIgnore
    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        InternalAttributeProviderConfigMap map = mapper.convertValue(props, InternalAttributeProviderConfigMap.class);

        // map all props defined in model
        setConfiguration(map);
    }

    @JsonIgnore
    @Override
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(InternalAttributeProviderConfigMap.class);
    }

}
