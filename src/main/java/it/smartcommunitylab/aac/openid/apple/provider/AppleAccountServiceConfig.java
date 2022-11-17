package it.smartcommunitylab.aac.openid.apple.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAccountServiceConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;

public class AppleAccountServiceConfig extends AbstractAccountServiceConfig<AppleIdentityProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_APPLE_SERIAL_VERSION;

    public AppleAccountServiceConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_APPLE, provider, realm, new AppleIdentityProviderConfigMap());
    }

    public AppleAccountServiceConfig(ConfigurableAccountProvider cp) {
        super(cp);
    }

    public String getRepositoryId() {
        // not configurable for now
        return getProvider();
    }

}
