package it.smartcommunitylab.aac.saml.provider;

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
public class SamlIdentityConfigurationProvider
        implements
        IdentityConfigurationProvider<SamlIdentityProviderConfig, SamlIdentityProviderConfigMap> {

    private SamlIdentityProviderConfigMap defaultConfig;

    public SamlIdentityConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
        if (authoritiesProperties != null && authoritiesProperties.getSaml() != null) {
            defaultConfig = authoritiesProperties.getSaml();
        } else {
            defaultConfig = new SamlIdentityProviderConfigMap();
        }
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_SAML;
    }

    @Override
    public SamlIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp, boolean mergeDefault) {
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

        return SamlIdentityProviderConfig.fromConfigurableProvider(cp);

    }

    @Override
    public SamlIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp) {
        return getConfig(cp, true);
    }

    @Override
    public SamlIdentityProviderConfigMap getDefaultConfigMap() {
        return defaultConfig;
    }

    @Override
    public SamlIdentityProviderConfigMap getConfigMap(Map<String, Serializable> map) {
        // return a valid config from props
        SamlIdentityProviderConfigMap config = new SamlIdentityProviderConfigMap();
        config.setConfiguration(map);
        return config;
    }

    @Override
    public JsonSchema getSchema() {
        try {
            return SamlIdentityProviderConfigMap.getConfigurationSchema();
        } catch (JsonMappingException e) {
            return null;
        }
    }

}
