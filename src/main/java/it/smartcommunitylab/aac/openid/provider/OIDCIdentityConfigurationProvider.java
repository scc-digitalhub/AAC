package it.smartcommunitylab.aac.openid.provider;

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
public class OIDCIdentityConfigurationProvider
        implements
        ConfigurationProvider<ConfigurableIdentityProvider, OIDCIdentityProviderConfig, OIDCIdentityProviderConfigMap> {

    private OIDCIdentityProviderConfigMap defaultConfig;

    public OIDCIdentityConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
        if (authoritiesProperties != null && authoritiesProperties.getOidc() != null) {
            defaultConfig = authoritiesProperties.getOidc();
        } else {
            defaultConfig = new OIDCIdentityProviderConfigMap();
        }
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_OIDC;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    @Override
    public OIDCIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp, boolean mergeDefault) {
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

        return OIDCIdentityProviderConfig.fromConfigurableProvider(cp);

    }

    @Override
    public OIDCIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp) {
        return getConfig(cp, true);
    }

    @Override
    public OIDCIdentityProviderConfigMap getDefaultConfigMap() {
        return defaultConfig;
    }

    @Override
    public OIDCIdentityProviderConfigMap getConfigMap(Map<String, Serializable> map) {
        // return a valid config from props
        OIDCIdentityProviderConfigMap config = new OIDCIdentityProviderConfigMap();
        config.setConfiguration(map);
        return config;
    }

    @Override
    public JsonSchema getSchema() {
        try {
            return OIDCIdentityProviderConfigMap.getConfigurationSchema();
        } catch (JsonMappingException e) {
            return null;
        }
    }

}
