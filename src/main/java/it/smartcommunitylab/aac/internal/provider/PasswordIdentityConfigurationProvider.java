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
import it.smartcommunitylab.aac.core.provider.IdentityConfigurationProvider;

@Service
public class PasswordIdentityConfigurationProvider
        implements
        IdentityConfigurationProvider<PasswordIdentityProviderConfig, PasswordIdentityProviderConfigMap> {

    private PasswordIdentityProviderConfigMap defaultConfig;

    public PasswordIdentityConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
        defaultConfig = new PasswordIdentityProviderConfigMap();
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
    public PasswordIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp, boolean mergeDefault) {
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

        return PasswordIdentityProviderConfig.fromConfigurableProvider(cp);

    }

    @Override
    public PasswordIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp) {
        return getConfig(cp, true);
    }

    @Override
    public PasswordIdentityProviderConfigMap getDefaultConfigMap() {
        return defaultConfig;
    }

    @Override
    public PasswordIdentityProviderConfigMap getConfigMap(Map<String, Serializable> map) {
        // return a valid config from props
        PasswordIdentityProviderConfigMap config = new PasswordIdentityProviderConfigMap();
        config.setConfiguration(map);
        return config;
    }

    @Override
    public JsonSchema getSchema() {
        try {
            return PasswordIdentityProviderConfigMap.getConfigurationSchema();
        } catch (JsonMappingException e) {
            return null;
        }
    }

}
