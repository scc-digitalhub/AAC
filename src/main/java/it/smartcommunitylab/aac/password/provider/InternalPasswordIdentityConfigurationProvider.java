package it.smartcommunitylab.aac.password.provider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.AuthoritiesProperties;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityConfigurationProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;

@Service
public class InternalPasswordIdentityConfigurationProvider
        implements
        IdentityConfigurationProvider<InternalPasswordIdentityProviderConfig, InternalPasswordIdentityProviderConfigMap> {

    private InternalPasswordIdentityProviderConfigMap defaultConfig;

    public InternalPasswordIdentityConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
        defaultConfig = new InternalPasswordIdentityProviderConfigMap();
        InternalIdentityProviderConfigMap internalConfig = new InternalIdentityProviderConfigMap();

        // read internal as base
        if (authoritiesProperties != null && authoritiesProperties.getInternal() != null) {
            internalConfig = authoritiesProperties.getInternal();
        }

        // extract non-empty props
        Map<String, Serializable> map = internalConfig.getConfiguration();

        // read local when available
        if (authoritiesProperties != null && authoritiesProperties.getPassword() != null) {
            map.putAll(authoritiesProperties.getPassword().getConfiguration());
        }

        defaultConfig.setConfiguration(map);
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_PASSWORD;
    }

    @Override
    public InternalPasswordIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp, boolean mergeDefault) {
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

        return InternalPasswordIdentityProviderConfig.fromConfigurableProvider(cp);

    }

    @Override
    public InternalPasswordIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp) {
        return getConfig(cp, true);
    }

    @Override
    public InternalPasswordIdentityProviderConfigMap getDefaultConfigMap() {
        return defaultConfig;
    }

    @Override
    public InternalPasswordIdentityProviderConfigMap getConfigMap(Map<String, Serializable> map) {
        // return a valid config from props
        InternalPasswordIdentityProviderConfigMap config = new InternalPasswordIdentityProviderConfigMap();
        config.setConfiguration(map);
        return config;
    }

    @Override
    public JsonSchema getSchema() {
        try {
            return InternalPasswordIdentityProviderConfigMap.getConfigurationSchema();
        } catch (JsonMappingException e) {
            return null;
        }
    }

}
