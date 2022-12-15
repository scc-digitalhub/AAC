package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAttributeProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

public class InternalAttributeProviderConfig
        extends AbstractAttributeProviderConfig<InternalAttributeProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
            + InternalAttributeProviderConfigMap.RESOURCE_TYPE;

    public InternalAttributeProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm, new InternalAttributeProviderConfigMap());
    }

    public InternalAttributeProviderConfig(ConfigurableAttributeProvider cp,
            InternalAttributeProviderConfigMap configMap) {
        super(cp, configMap);
    }

    public boolean isUsermode() {
        return configMap.getUsermode() != null ? configMap.getUsermode().booleanValue() : false;
    }

}
