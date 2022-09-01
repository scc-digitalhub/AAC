package it.smartcommunitylab.aac.openid.provider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.AuthoritiesProperties;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityConfigurationProvider;

@Service
public class OIDCIdentityConfigurationProvider
        implements
        IdentityConfigurationProvider<OIDCIdentityProviderConfig, OIDCIdentityProviderConfigMap> {

    private final String authority;
    private OIDCIdentityProviderConfigMap defaultConfig;

    @Autowired
    public OIDCIdentityConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
        this.authority = SystemKeys.AUTHORITY_OIDC;
        if (authoritiesProperties != null && authoritiesProperties.getOidc() != null) {
            defaultConfig = authoritiesProperties.getOidc();
        } else {
            defaultConfig = new OIDCIdentityProviderConfigMap();
        }

    }

    public OIDCIdentityConfigurationProvider(String authority, OIDCIdentityProviderConfigMap configMap) {
        Assert.hasText(authority, "authority id is mandatory");
        this.authority = authority;
        this.defaultConfig = configMap;
    }

    @Override
    public String getAuthority() {
        return authority;
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
