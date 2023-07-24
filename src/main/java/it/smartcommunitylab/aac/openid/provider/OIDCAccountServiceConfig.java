package it.smartcommunitylab.aac.openid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAccountServiceConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;

public class OIDCAccountServiceConfig extends AbstractAccountServiceConfig<OIDCIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_ACCOUNT_SERVICE +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_OIDC;

    public OIDCAccountServiceConfig(String provider, String realm) {
        this(SystemKeys.AUTHORITY_OIDC, provider, realm);
    }

    public OIDCAccountServiceConfig(String authority, String provider, String realm) {
        super(authority, provider, realm, new OIDCIdentityProviderConfigMap());
    }

    public OIDCAccountServiceConfig(ConfigurableAccountProvider cp, OIDCIdentityProviderConfigMap configMap) {
        super(cp, configMap);
    }

    public String getRepositoryId() {
        // not configurable for now
        return getProvider();
    }
}
