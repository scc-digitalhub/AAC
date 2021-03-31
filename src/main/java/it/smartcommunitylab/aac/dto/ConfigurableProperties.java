package it.smartcommunitylab.aac.dto;

import java.util.Map;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

/*
 * A bean for configurable properties, which can convert to/from map and describe a schema
 * 
 * Should be used to carry implementation specific properties over generic interfaces, replacing all the Map<> in base/default models
 */
public abstract class ConfigurableProperties {

    public abstract Map<String, String> getProperties();

    public abstract void setProperties(Map<String, String> props);

    public abstract JsonSchema getSchema();

}
