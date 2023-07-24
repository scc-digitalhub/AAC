package it.smartcommunitylab.aac.core.model;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

/*
 * A configMap is a configuration object with a well-defined schema
 * which should also support validation
 *
 * TODO add validation
 */
public interface ConfigMap extends ConfigurableProperties {
    public JsonSchema getSchema() throws JsonMappingException;
}
