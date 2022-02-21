package it.smartcommunitylab.aac.webauthn.provider;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableIdentityProvider;

public class WebAuthnIdentityProviderConfig extends AbstractConfigurableProvider {

    private String name;
    private String description;
    private String icon;
    private Boolean linkable;
    private String displayMode;

    // map capabilities
    private WebAuthnIdentityProviderConfigMap configMap;

    // hook functions
    private Map<String, String> hookFunctions;

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    public WebAuthnIdentityProviderConfig(String provider, String realm) {
        this(SystemKeys.AUTHORITY_WEBAUTHN, provider, realm);
    }

    protected WebAuthnIdentityProviderConfig(String authority, String provider, String realm) {
        super(authority, provider, realm);
        this.configMap = new WebAuthnIdentityProviderConfigMap();
        this.hookFunctions = Collections.emptyMap();
    }

    @Override
    public Map<String, Serializable> getConfiguration() {
        return configMap.getConfiguration();
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        configMap = new WebAuthnIdentityProviderConfigMap();
        configMap.setConfiguration(props);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    public Map<String, String> getHookFunctions() {
        return hookFunctions;
    }

    public void setHookFunctions(Map<String, String> hookFunctions) {
        this.hookFunctions = hookFunctions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(String displayMode) {
        this.displayMode = displayMode;
    }

    public Boolean getLinkable() {
        return linkable;
    }

    public void setLinkable(Boolean linkable) {
        this.linkable = linkable;
    }

    public boolean isLinkable() {
        if (linkable != null) {
            return linkable.booleanValue();
        }

        return true;
    }

    public void setConfigMap(WebAuthnIdentityProviderConfigMap configMap) {
        this.configMap = configMap;
    }

    public WebAuthnIdentityProviderConfigMap getConfigMap() {
        return configMap;
    }

    public static ConfigurableIdentityProvider toConfigurableProvider(WebAuthnIdentityProviderConfig ip) {
        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider(SystemKeys.AUTHORITY_WEBAUTHN,
                ip.getProvider(),
                ip.getRealm());
        cp.setType(SystemKeys.RESOURCE_IDENTITY);
        cp.setPersistence(SystemKeys.PERSISTENCE_LEVEL_REPOSITORY);

        cp.setName(ip.getName());
        cp.setDescription(ip.getDescription());
        cp.setIcon(ip.getIcon());
        cp.setDisplayMode(ip.getDisplayMode());

        cp.setEnabled(true);
        cp.setLinkable(ip.isLinkable());
        cp.setConfiguration(ip.getConfigMap().getConfiguration());
        cp.setHookFunctions(ip.getHookFunctions());

        return cp;
    }

    public static WebAuthnIdentityProviderConfig fromConfigurableProvider(ConfigurableIdentityProvider cp) {
        WebAuthnIdentityProviderConfig ip = new WebAuthnIdentityProviderConfig(cp.getProvider(), cp.getRealm());
        ip.configMap = new WebAuthnIdentityProviderConfigMap();
        ip.configMap.setConfiguration(cp.getConfiguration());

        ip.name = cp.getName();
        ip.description = cp.getDescription();
        ip.icon = cp.getIcon();
        ip.displayMode = cp.getDisplayMode();

        ip.linkable = cp.isLinkable();
        ip.hookFunctions = (cp.getHookFunctions() != null ? cp.getHookFunctions() : Collections.emptyMap());

        return ip;
    }

    public static WebAuthnIdentityProviderConfig fromConfigurableProvider(ConfigurableIdentityProvider cp,
            WebAuthnIdentityProviderConfigMap defaultConfigMap) {
        WebAuthnIdentityProviderConfig ip = fromConfigurableProvider(cp);

        // double conversion via map to merge default props
        Map<String, Serializable> config = new HashMap<>();
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        Map<String, Serializable> defaultMap = mapper.convertValue(defaultConfigMap, typeRef);
        config.putAll(defaultMap);
        config.putAll(cp.getConfiguration());

        ip.configMap.setConfiguration(config);
        return ip;
    }

    public boolean isAllowedUnstrustedAssertions() {
        return configMap.getTrustUnverifiedAuthenticatorResponses() != null
                ? configMap.getTrustUnverifiedAuthenticatorResponses().booleanValue()
                : false;
    }

}
