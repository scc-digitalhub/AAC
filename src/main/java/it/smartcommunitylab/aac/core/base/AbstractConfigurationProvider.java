package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfig;
import it.smartcommunitylab.aac.repository.ConfigMapConverter;

public abstract class AbstractConfigurationProvider<M extends AbstractConfigMap, T extends ConfigurableProvider, C extends ProviderConfig<M, T>>
        implements ConfigurationProvider<M, T, C> {

    protected final String authority;
    protected final ConfigMapConverter<M> configMapConverter = new ConfigMapConverter<>();
    protected M defaultConfigMap;

    public AbstractConfigurationProvider(String authority) {
        Assert.hasText(authority, "authority id is mandatory");
        this.authority = authority;
    }

    protected void setDefaultConfigMap(M defaultConfigMap) {
        this.defaultConfigMap = defaultConfigMap;
    }

    protected abstract C buildConfig(T cp);

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public C getConfig(T cp, boolean mergeDefault) {
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
    public C getConfig(T cp) {
        return getConfig(cp, true);
    }

    @Override
    public M getDefaultConfigMap() {
        return defaultConfigMap;
    }

    @Override
    public M getConfigMap(Map<String, Serializable> map) {
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
