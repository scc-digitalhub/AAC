package it.smartcommunitylab.aac.attributes.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAttributeProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

public class ScriptAttributeProviderConfig extends AbstractAttributeProviderConfig<ScriptAttributeProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
            + ScriptAttributeProviderConfigMap.RESOURCE_TYPE;

    public ScriptAttributeProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_SCRIPT, provider, realm, new ScriptAttributeProviderConfigMap());
    }

    public ScriptAttributeProviderConfig(ConfigurableAttributeProvider cp, ScriptAttributeProviderConfigMap configMap) {
        super(cp, configMap);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     * 
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private ScriptAttributeProviderConfig() {
        super(SystemKeys.AUTHORITY_SCRIPT, (String) null, (String) null, new ScriptAttributeProviderConfigMap());
    }
}
