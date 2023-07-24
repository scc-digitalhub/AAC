package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;
import java.util.Map;

/*
 * A bean for configurable properties, which can convert to/from map and describe a schema
 *
 * Should be used to carry implementation specific properties over generic interfaces, replacing all the Map<> in base/default models
 */
public interface ConfigurableProperties {
    public Map<String, Serializable> getConfiguration();

    public void setConfiguration(Map<String, Serializable> props);
    //    public JsonSchema getSchema();

}
