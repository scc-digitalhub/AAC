package it.smartcommunitylab.aac.core.provider;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;

/*
 * Expose provider configuration outside modules
 */
public interface ConfigurationProvider<M extends ConfigMap, T extends ConfigurableProvider, C extends ProviderConfig<M, T>> {

    public String getAuthority();

    default public String getType() {
        return SystemKeys.RESOURCE_CONFIG;
    }

    /*
     * Translate a configurableProvider with valid props to a valid provider config,
     * with default values set
     */
    public C getConfig(T cp);

    public C getConfig(T cp, boolean mergeDefault);

    /*
     * Expose and translate to valid configMap
     */

    public M getDefaultConfigMap();

    public M getConfigMap(Map<String, Serializable> map);

    /*
     * Validate configuration against schema and also policies (TODO)
     */
//    public boolean isConfigValid(T cp);
//
//    public boolean isConfigMapValid(M configMap);

    /*
     * Export the configuration schema for configMap
     * 
     * this schema should match M but could include descriptions, localized labels
     * etc and should be used for API doc, UI forms etc
     */

    public JsonSchema getSchema();

}
