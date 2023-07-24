package it.smartcommunitylab.aac.saml.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAccountServiceConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;

public class SamlAccountServiceConfig extends AbstractAccountServiceConfig<SamlIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_ACCOUNT_SERVICE +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_SAML;

    public SamlAccountServiceConfig(String provider, String realm) {
        this(SystemKeys.AUTHORITY_SAML, provider, realm);
    }

    public SamlAccountServiceConfig(String authority, String provider, String realm) {
        super(authority, provider, realm, new SamlIdentityProviderConfigMap());
    }

    public SamlAccountServiceConfig(ConfigurableAccountProvider cp, SamlIdentityProviderConfigMap configMap) {
        super(cp, configMap);
    }

    public String getRepositoryId() {
        // not configurable for now
        return getProvider();
    }
}
