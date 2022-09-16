package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

public class InternalIdentityProviderConfig extends AbstractIdentityProviderConfig<InternalIdentityProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final static int MAX_SESSION_DURATION = 24 * 60 * 60; // 24h

    public InternalIdentityProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm, new InternalIdentityProviderConfigMap());
    }

    public InternalIdentityProviderConfig(ConfigurableIdentityProvider cp) {
        super(cp);
    }

    public String getRepositoryId() {
        return configMap.getRepositoryId() != null ? configMap.getRepositoryId() : getRealm();
    }

    /*
     * default config
     */

    public int getMaxSessionDuration() {
        return configMap.getMaxSessionDuration() != null ? configMap.getMaxSessionDuration().intValue()
                : MAX_SESSION_DURATION;
    }

}
