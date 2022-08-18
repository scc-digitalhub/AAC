package it.smartcommunitylab.aac.spid.provider;

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
public class SpidIdentityConfigurationProvider
        implements
        IdentityConfigurationProvider<SpidIdentityProviderConfig, SpidIdentityProviderConfigMap> {

    private SpidIdentityProviderConfigMap defaultConfig;

    public SpidIdentityConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
        if (authoritiesProperties != null && authoritiesProperties.getSpid() != null) {
            defaultConfig = authoritiesProperties.getSpid();
        } else {
            defaultConfig = new SpidIdentityProviderConfigMap();
        }
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_SPID;
    }

    @Override
    public SpidIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp, boolean mergeDefault) {
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

        return SpidIdentityProviderConfig.fromConfigurableProvider(cp);

    }

    @Override
    public SpidIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp) {
        return getConfig(cp, true);
    }

    @Override
    public SpidIdentityProviderConfigMap getDefaultConfigMap() {
        return defaultConfig;
    }

    @Override
    public SpidIdentityProviderConfigMap getConfigMap(Map<String, Serializable> map) {
        // return a valid config from props
        SpidIdentityProviderConfigMap config = new SpidIdentityProviderConfigMap();
        config.setConfiguration(map);
        return config;
    }

    @Override
    public JsonSchema getSchema() {
        try {
            return SpidIdentityProviderConfigMap.getConfigurationSchema();
        } catch (JsonMappingException e) {
            return null;
        }
    }

}
