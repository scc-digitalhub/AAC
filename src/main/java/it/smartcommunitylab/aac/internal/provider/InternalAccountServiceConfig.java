package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAccountServiceConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountService;

public class InternalAccountServiceConfig extends AbstractAccountServiceConfig<InternalIdentityProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final static int DEFAULT_CONFIRM_DURATION = 24 * 60 * 60; // 24h

    public InternalAccountServiceConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm, new InternalIdentityProviderConfigMap());
    }

    public InternalAccountServiceConfig(ConfigurableAccountService cp) {
        super(cp);
    }

    public String getRepositoryId() {
        // not configurable for now
        return getRealm();
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

    public boolean isEnableDelete() {
        return configMap.getEnableDelete() != null ? configMap.getEnableDelete().booleanValue() : true;
    }

    public boolean isConfirmationRequired() {
        return configMap.getConfirmationRequired() != null ? configMap.getConfirmationRequired().booleanValue() : true;
    }

    /*
     * default config
     */
    public int getConfirmationValidity() {
        return configMap.getConfirmationValidity() != null ? configMap.getConfirmationValidity().intValue()
                : DEFAULT_CONFIRM_DURATION;
    }

}
