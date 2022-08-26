package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;
import it.smartcommunitylab.aac.repository.ConfigMapConverter;

public abstract class AbstractConfigurationProvider<T extends ConfigurableProvider, C extends AbstractProviderConfig<P>, P extends AbstractConfigMap>
        implements ConfigurationProvider<T, C, P> {

    protected final String authority;
    protected final ConfigMapConverter<P> configMapConverter = new ConfigMapConverter<>();
    protected P defaultConfigMap;

    public AbstractConfigurationProvider(String authority) {
        Assert.hasText(authority, "authority id is mandatory");
        this.authority = authority;
    }

    protected void setDefaultConfigMap(P defaultConfigMap) {
        this.defaultConfigMap = defaultConfigMap;
    }

    protected abstract C buildConfig(ConfigurableProvider cp);

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public C getConfig(ConfigurableProvider cp, boolean mergeDefault) {
        if (mergeDefault && defaultConfigMap != null) {
            // merge configMap with default on missing values
            Map<String, Serializable> map = new HashMap<>();
            map.putAll(cp.getConfiguration());

            Map<String, Serializable> defaultMap = defaultConfigMap.getConfiguration();
            defaultMap.entrySet().forEach(e -> {
                map.putIfAbsent(e.getKey(), e.getValue());
            });

            cp.setConfiguration(map);
        }

        return buildConfig(cp);

    }

    @Override
    public C getConfig(ConfigurableProvider cp) {
        return getConfig(cp, true);
    }

    @Override
    public P getDefaultConfigMap() {
        return defaultConfigMap;
    }

    @Override
    public P getConfigMap(Map<String, Serializable> map) {
        // return a valid config from props
        return configMapConverter.convert(map);
    }

    @Override
    public JsonSchema getSchema() {
        try {
            return defaultConfigMap.getSchema();
        } catch (JsonMappingException e) {
            return null;
        }
    }

}
