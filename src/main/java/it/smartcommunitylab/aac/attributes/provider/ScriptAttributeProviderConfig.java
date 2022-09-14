package it.smartcommunitylab.aac.attributes.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAttributeProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

public class ScriptAttributeProviderConfig extends AbstractAttributeProviderConfig<ScriptAttributeProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    public ScriptAttributeProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_SCRIPT, provider, realm, new ScriptAttributeProviderConfigMap());
    }

    public ScriptAttributeProviderConfig(ConfigurableAttributeProvider cp) {
        super(cp);
    }

}
