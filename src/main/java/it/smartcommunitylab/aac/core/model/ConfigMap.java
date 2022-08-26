package it.smartcommunitylab.aac.core.model;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

public interface ConfigMap extends ConfigurableProperties {

    public abstract JsonSchema getSchema() throws JsonMappingException;

}
