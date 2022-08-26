package it.smartcommunitylab.aac.internal.provider;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAttributeProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

public class InternalAttributeProviderConfig extends AbstractAttributeProviderConfig {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    // map capabilities
    private InternalAttributeProviderConfigMap configMap;

    public InternalAttributeProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm);
        this.configMap = new InternalAttributeProviderConfigMap();
    }

    public InternalAttributeProviderConfigMap getConfigMap() {
        return configMap;
    }

    public void setConfigMap(InternalAttributeProviderConfigMap configMap) {
        this.configMap = configMap;
    }

    @Override
    public Map<String, Serializable> getConfiguration() {
        return configMap.getConfiguration();
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        configMap = new InternalAttributeProviderConfigMap();
        configMap.setConfiguration(props);
    }

    /*
     * builders
     */
    public static InternalAttributeProviderConfig fromConfigurableProvider(ConfigurableAttributeProvider cp) {
        InternalAttributeProviderConfig ap = new InternalAttributeProviderConfig(cp.getProvider(), cp.getRealm());
        ap.configMap = new InternalAttributeProviderConfigMap();
        ap.configMap.setConfiguration(cp.getConfiguration());

        ap.name = cp.getName();
        ap.description = cp.getDescription();

        ap.attributeSets = cp.getAttributeSets() != null ? cp.getAttributeSets() : Collections.emptySet();
        return ap;
    }

}
