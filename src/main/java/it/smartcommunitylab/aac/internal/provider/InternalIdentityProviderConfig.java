package it.smartcommunitylab.aac.internal.provider;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.internal.model.CredentialsType;

public class InternalIdentityProviderConfig extends AbstractIdentityProviderConfig {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected static ObjectMapper mapper = new ObjectMapper();
    protected final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    private final static int MIN_DURATION = 300;

    // map capabilities
    private InternalIdentityProviderConfigMap configMap;

    public InternalIdentityProviderConfig(String provider, String realm) {
        this(SystemKeys.AUTHORITY_INTERNAL, provider, realm);
    }

    public InternalIdentityProviderConfig(String authority, String provider, String realm) {
        super(authority, provider, realm);
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

    public String getRepositoryId() {
        // isolated providers will use their id as providerId for data repositories
        // otherwise they'll expose realm slug as id
        if (isolateData()) {
            return this.getProvider();
        } else {
            return this.getRealm();
        }
    }

    public CredentialsType getCredentialsType() {
        return configMap.getCredentialsType() != null ? configMap.getCredentialsType() : CredentialsType.PASSWORD;
    }

    public boolean isolateData() {
        return configMap.getIsolateData() != null ? configMap.getIsolateData().booleanValue() : false;
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

    public boolean isConfirmationRequired() {
        return configMap.getConfirmationRequired() != null ? configMap.getConfirmationRequired().booleanValue() : true;
    }

    /*
     * default config
     */
    public int getConfirmationValidity() {
        return configMap.getConfirmationValidity() != null ? configMap.getConfirmationValidity().intValue()
                : MIN_DURATION;
    }

    /*
     * Static parser
     */
    public static InternalIdentityProviderConfig fromConfigurableProvider(ConfigurableIdentityProvider cp) {
        InternalIdentityProviderConfig ip = new InternalIdentityProviderConfig(cp.getProvider(), cp.getRealm());
        ip.configMap = new InternalIdentityProviderConfigMap();
        ip.configMap.setConfiguration(cp.getConfiguration());

        ip.name = cp.getName();
        ip.description = cp.getDescription();
        ip.icon = cp.getIcon();

        ip.persistence = cp.getPersistence();
        ip.linkable = cp.isLinkable();
        ip.hookFunctions = (cp.getHookFunctions() != null ? cp.getHookFunctions() : Collections.emptyMap());

        return ip;
    }

}
