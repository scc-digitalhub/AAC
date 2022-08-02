package it.smartcommunitylab.aac.webauthn.provider;

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
public class WebAuthnIdentityConfigurationProvider
        implements
        IdentityConfigurationProvider<WebAuthnIdentityProviderConfig, WebAuthnIdentityProviderConfigMap> {

    private WebAuthnIdentityProviderConfigMap defaultConfig;

    public WebAuthnIdentityConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
        defaultConfig = new WebAuthnIdentityProviderConfigMap();
        InternalIdentityProviderConfigMap internalConfig = new InternalIdentityProviderConfigMap();

        // read internal as base
        if (authoritiesProperties != null && authoritiesProperties.getInternal() != null) {
            internalConfig = authoritiesProperties.getInternal();
        }

        // extract non-empty props
        Map<String, Serializable> map = internalConfig.getConfiguration();

        // read local when available
        if (authoritiesProperties != null && authoritiesProperties.getWebauthn() != null) {
            map.putAll(authoritiesProperties.getWebauthn().getConfiguration());
        }

        defaultConfig.setConfiguration(map);
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_WEBAUTHN;
    }

    @Override
    public WebAuthnIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp, boolean mergeDefault) {
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

        return WebAuthnIdentityProviderConfig.fromConfigurableProvider(cp);

    }

    @Override
    public WebAuthnIdentityProviderConfig getConfig(ConfigurableIdentityProvider cp) {
        return getConfig(cp, true);
    }

    @Override
    public WebAuthnIdentityProviderConfigMap getDefaultConfigMap() {
        return defaultConfig;
    }

    @Override
    public WebAuthnIdentityProviderConfigMap getConfigMap(Map<String, Serializable> map) {
        // return a valid config from props
        WebAuthnIdentityProviderConfigMap config = new WebAuthnIdentityProviderConfigMap();
        config.setConfiguration(map);
        return config;
    }

    @Override
    public JsonSchema getSchema() {
        try {
            return WebAuthnIdentityProviderConfigMap.getConfigurationSchema();
        } catch (JsonMappingException e) {
            return null;
        }
    }

}
