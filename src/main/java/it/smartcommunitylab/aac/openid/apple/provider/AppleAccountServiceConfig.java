package it.smartcommunitylab.aac.openid.apple.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAccountServiceConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;

public class AppleAccountServiceConfig extends AbstractAccountServiceConfig<AppleIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_APPLE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_ACCOUNT_SERVICE +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_APPLE;

    public AppleAccountServiceConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_APPLE, provider, realm, new AppleIdentityProviderConfigMap());
    }

    public AppleAccountServiceConfig(ConfigurableAccountProvider cp, AppleIdentityProviderConfigMap configMap) {
        super(cp, configMap);
    }

    public String getRepositoryId() {
        // not configurable for now
        return getProvider();
    }
}
