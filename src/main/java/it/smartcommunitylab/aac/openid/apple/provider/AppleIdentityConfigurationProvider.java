package it.smartcommunitylab.aac.openid.apple.provider;

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
public class AppleIdentityConfigurationProvider
        implements
        IdentityConfigurationProvider<AppleIdentityProviderConfig, AppleIdentityProviderConfigMap> {

    private AppleIdentityProviderConfigMap defaultConfig;

    public AppleIdentityConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
        if (authoritiesProperties != null && authoritiesProperties.getApple() != null) {
            defaultConfig = authoritiesProperties.getApple();
        } else {
            defaultConfig = new AppleIdentityProviderConfigMap();
        }
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_APPLE;
    }

    @Override
    public AppleIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp, boolean mergeDefault) {
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

        return AppleIdentityProviderConfig.fromConfigurableProvider(cp);

    }

    @Override
    public AppleIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp) {
        return getConfig(cp, true);
    }

    @Override
    public AppleIdentityProviderConfigMap getDefaultConfigMap() {
        return defaultConfig;
    }

    @Override
    public AppleIdentityProviderConfigMap getConfigMap(Map<String, Serializable> map) {
        // return a valid config from props
        AppleIdentityProviderConfigMap config = new AppleIdentityProviderConfigMap();
        config.setConfiguration(map);
        return config;
    }

    @Override
    public JsonSchema getSchema() {
        try {
            return AppleIdentityProviderConfigMap.getConfigurationSchema();
        } catch (JsonMappingException e) {
            return null;
        }
    }

}
