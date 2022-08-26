package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

public class InternalIdentityProviderConfig extends AbstractIdentityProviderConfig<InternalIdentityProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final static int MIN_DURATION = 300;
    private final static int MAX_SESSION_DURATION = 24 * 60 * 60; // 24h

    public InternalIdentityProviderConfig(String provider, String realm) {
        this(SystemKeys.AUTHORITY_INTERNAL, provider, realm);
    }

    public InternalIdentityProviderConfig(String authority, String provider, String realm) {
        super(authority, provider, realm, new InternalIdentityProviderConfigMap());
    }

    public InternalIdentityProviderConfig(ConfigurableIdentityProvider cp) {
        super(cp);
    }

    public String getRepositoryId() {
        // scoped providers will use their id as providerId for data repositories
        // otherwise they'll expose realm slug as id
        if (isScopedData()) {
            return this.getProvider();
        } else {
            return this.getRealm();
        }
    }

//    public CredentialsType getCredentialsType() {
//        return configMap.getCredentialsType() != null ? configMap.getCredentialsType() : CredentialsType.NONE;
//    }

    public boolean isScopedData() {
        return configMap.getScopedData() != null ? configMap.getScopedData().booleanValue() : false;
    }

    public String getScope() {
        if (isScopedData()) {
            return SystemKeys.RESOURCE_PROVIDER;
        }

        return SystemKeys.RESOURCE_REALM;
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

    public int getMaxSessionDuration() {
        return configMap.getMaxSessionDuration() != null ? configMap.getMaxSessionDuration().intValue()
                : MAX_SESSION_DURATION;
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

        ip.linkable = cp.isLinkable();
        ip.persistence = cp.getPersistence();
        ip.events = cp.getEvents();
        ip.position = cp.getPosition();

        ip.hookFunctions = (cp.getHookFunctions() != null ? cp.getHookFunctions() : Collections.emptyMap());

        return ip;
    }

}
