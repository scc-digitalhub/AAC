package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityServiceConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;

public class InternalIdentityServiceConfig extends AbstractIdentityServiceConfig<InternalIdentityServiceConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    public InternalIdentityServiceConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm, new InternalIdentityServiceConfigMap());
    }

    public InternalIdentityServiceConfig(ConfigurableIdentityService cp) {
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

}