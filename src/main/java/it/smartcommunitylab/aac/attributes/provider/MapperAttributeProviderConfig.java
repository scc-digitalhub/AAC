package it.smartcommunitylab.aac.attributes.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.mapper.DefaultAttributesMapper;
import it.smartcommunitylab.aac.core.base.AbstractAttributeProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

public class MapperAttributeProviderConfig extends AbstractAttributeProviderConfig<MapperAttributeProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
            + MapperAttributeProviderConfigMap.RESOURCE_TYPE;

    public MapperAttributeProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_MAPPER, provider, realm, new MapperAttributeProviderConfigMap());
    }

    public MapperAttributeProviderConfig(ConfigurableAttributeProvider cp, MapperAttributeProviderConfigMap configMap) {
        super(cp, configMap);
    }

    public String getMapperType() {
        return configMap.getType() != null ? configMap.getType() : DefaultAttributesMapper.TYPE;
    }

}
