package it.smartcommunitylab.aac.internal.provider;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.base.ConfigurableIdentityProvider;

public class InternalIdentityProviderConfig extends AbstractIdentityProviderConfig {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };
    private final static int MIN_DURATION = 300;
    private final static int PASSWORD_MIN_LENGTH = 2;
    private final static int PASSWORD_MAX_LENGTH = 75;

    // map capabilities
    private InternalIdentityProviderConfigMap configMap;

    public InternalIdentityProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm);
        this.configMap = new InternalIdentityProviderConfigMap();
    }

    public InternalIdentityProviderConfigMap getConfigMap() {
        return configMap;
    }

    public void setConfigMap(InternalIdentityProviderConfigMap configMap) {
        this.configMap = configMap;
    }

    @Override
    public Map<String, Serializable> getConfiguration() {
        return configMap.getConfiguration();
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        configMap = new InternalIdentityProviderConfigMap();
        configMap.setConfiguration(props);
    }

    /*
     * config flags
     */
    public boolean isEnableRegistration() {
        return configMap.getEnableRegistration() != null ? configMap.getEnableRegistration().booleanValue() : true;
    }

    public boolean isEnableUpdate() {
        return configMap.getEnableUpdate() != null ? configMap.getEnableUpdate().booleanValue() : true;
    }

    public boolean isEnablePasswordReset() {
        return configMap.getEnablePasswordReset() != null ? configMap.getEnablePasswordReset().booleanValue() : true;
    }

    public boolean isEnablePasswordSet() {
        return configMap.getEnablePasswordSet() != null ? configMap.getEnablePasswordSet().booleanValue() : true;
    }

    public boolean isConfirmationRequired() {
        return configMap.getConfirmationRequired() != null ? configMap.getConfirmationRequired().booleanValue() : true;
    }

    public boolean isPasswordRequireAlpha() {
        return configMap.getPasswordRequireAlpha() != null ? configMap.getPasswordRequireAlpha().booleanValue() : false;
    }

    public boolean isPasswordRequireNumber() {
        return configMap.getPasswordRequireNumber() != null ? configMap.getPasswordRequireNumber().booleanValue()
                : false;
    }

    public boolean isPasswordRequireSpecial() {
        return configMap.getPasswordRequireSpecial() != null ? configMap.getPasswordRequireSpecial().booleanValue()
                : false;
    }

    public boolean isPasswordSupportWhitespace() {
        return configMap.getPasswordSupportWhitespace() != null
                ? configMap.getPasswordSupportWhitespace().booleanValue()
                : false;
    }

    /*
     * default config
     */
    public int getConfirmationValidity() {
        return configMap.getConfirmationValidity() != null ? configMap.getConfirmationValidity().intValue()
                : MIN_DURATION;
    }

    public int getPasswordResetValidity() {
        return configMap.getPasswordResetValidity() != null ? configMap.getPasswordResetValidity().intValue()
                : MIN_DURATION;
    }

    public int getPasswordMinLength() {
        return configMap.getPasswordMinLength() != null ? configMap.getPasswordMinLength().intValue()
                : PASSWORD_MIN_LENGTH;
    }

    public int getPasswordMaxLength() {
        return configMap.getPasswordMaxLength() != null ? configMap.getPasswordMaxLength().intValue()
                : PASSWORD_MAX_LENGTH;
    }
    /*
     * builders
     */
//    public static ConfigurableIdentityProvider toConfigurableProvider(InternalIdentityProviderConfig ip) {
//        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider(SystemKeys.AUTHORITY_INTERNAL,
//                ip.getProvider(),
//                ip.getRealm());
//        cp.setType(SystemKeys.RESOURCE_IDENTITY);
//        cp.setPersistence(SystemKeys.PERSISTENCE_LEVEL_REPOSITORY);
//
//        cp.setName(ip.getName());
//        cp.setDescription(ip.getDescription());
//        cp.setIcon(ip.getIcon());
//        cp.setDisplayMode(ip.getDisplayMode());
//
//        cp.setEnabled(true);
//        cp.setLinkable(ip.isLinkable());
//        cp.setConfiguration(ip.getConfigMap().getConfiguration());
//        cp.setHookFunctions(ip.getHookFunctions());
//
//        return cp;
//    }

    public static InternalIdentityProviderConfig fromConfigurableProvider(ConfigurableIdentityProvider cp) {
        InternalIdentityProviderConfig ip = new InternalIdentityProviderConfig(cp.getProvider(), cp.getRealm());
        ip.configMap = new InternalIdentityProviderConfigMap();
        ip.configMap.setConfiguration(cp.getConfiguration());

        ip.name = cp.getName();
        ip.description = cp.getDescription();
        ip.icon = cp.getIcon();
        ip.displayMode = cp.getDisplayMode();

        ip.persistence = cp.getPersistence();
        ip.linkable = cp.isLinkable();
        ip.hookFunctions = (cp.getHookFunctions() != null ? cp.getHookFunctions() : Collections.emptyMap());

        return ip;
    }

    public static InternalIdentityProviderConfig fromConfigurableProvider(ConfigurableIdentityProvider cp,
            InternalIdentityProviderConfigMap defaultConfigMap) {
        InternalIdentityProviderConfig ip = fromConfigurableProvider(cp);

        // double conversion via map to merge default props
        Map<String, Serializable> config = new HashMap<>();
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        Map<String, Serializable> defaultMap = mapper.convertValue(defaultConfigMap, typeRef);
        config.putAll(defaultMap);
        config.putAll(cp.getConfiguration());

        ip.configMap.setConfiguration(config);
        return ip;
    }

}
