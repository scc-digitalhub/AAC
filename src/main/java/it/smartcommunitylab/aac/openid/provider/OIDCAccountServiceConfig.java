package it.smartcommunitylab.aac.openid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAccountServiceConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;

public class OIDCAccountServiceConfig extends AbstractAccountServiceConfig<OIDCIdentityProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    public OIDCAccountServiceConfig(String provider, String realm) {
        this(SystemKeys.AUTHORITY_OIDC, provider, realm);
    }

    public OIDCAccountServiceConfig(String authority, String provider, String realm) {
        super(authority, provider, realm, new OIDCIdentityProviderConfigMap());
    }

    public OIDCAccountServiceConfig(ConfigurableAccountProvider cp) {
        super(cp);
    }

    public String getRepositoryId() {
        // not configurable for now
        return getProvider();
    }

}
