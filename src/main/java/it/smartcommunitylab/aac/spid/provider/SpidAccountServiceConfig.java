package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.base.AbstractAccountServiceConfig;
import it.smartcommunitylab.aac.accounts.provider.AccountServiceSettingsMap;

// TODO:  non ho la minima idea di che problema questa classe risolva. Questa babele di classi config
//  prima o poi mi ucciderà
public class SpidAccountServiceConfig extends AbstractAccountServiceConfig<SpidIdentityProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_ACCOUNT_SERVICE +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_SPID;

    public SpidAccountServiceConfig(String authority, String provider, String realm) {
        super(authority, provider, realm, new AccountServiceSettingsMap(), new SpidIdentityProviderConfigMap());
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */

    @SuppressWarnings("unused")
    private SpidAccountServiceConfig() {
        super();
    }

    @Override
    public String getRepositoryId() {
        // not configurable for now
        return getProvider();
    }

}
