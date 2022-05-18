package it.smartcommunitylab.aac.internal.provider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.AuthoritiesProperties;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;

@Service
public class InternalIdentityConfigurationProvider
        implements
        ConfigurationProvider<ConfigurableIdentityProvider, InternalIdentityProviderConfig, InternalIdentityProviderConfigMap> {

    private InternalIdentityProviderConfigMap defaultConfig;

    public InternalIdentityConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
        if (authoritiesProperties != null && authoritiesProperties.getInternal() != null) {
            defaultConfig = authoritiesProperties.getInternal();
        } else {
            defaultConfig = new InternalIdentityProviderConfigMap();
        }
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_INTERNAL;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    @Override
    public InternalIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp, boolean mergeDefault) {
        if (mergeDefault) {
            // merge configMap with default on missing values
            Map<String, Serializable> map = new HashMap<>();
            map.putAll(cp.getConfiguration());

            Map<String, Serializable> defaultMap = defaultConfig.getConfiguration();
            defaultMap.entrySet().forEach(e -> {
                map.putIfAbsent(e.getKey(), e.getValue());
            });

            cp.setConfiguration(map);
        }

        return InternalIdentityProviderConfig.fromConfigurableProvider(cp);

    }

    @Override
    public InternalIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp) {
        return getConfig(cp, true);
    }

    @Override
    public InternalIdentityProviderConfigMap getDefaultConfigMap() {
        return defaultConfig;
    }

    @Override
    public InternalIdentityProviderConfigMap getConfigMap(Map<String, Serializable> map) {
        // return a valid config from props
        InternalIdentityProviderConfigMap config = new InternalIdentityProviderConfigMap();
        config.setConfiguration(map);
        return config;
    }

    @Override
    public JsonSchema getSchema() {
        try {
            return InternalIdentityProviderConfigMap.getConfigurationSchema();
        } catch (JsonMappingException e) {
            return null;
        }
    }

}
