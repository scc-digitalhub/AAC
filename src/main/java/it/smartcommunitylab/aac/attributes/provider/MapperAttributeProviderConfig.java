package it.smartcommunitylab.aac.attributes.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.mapper.DefaultAttributesMapper;
import it.smartcommunitylab.aac.core.base.AbstractAttributeProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

public class MapperAttributeProviderConfig extends AbstractAttributeProviderConfig<MapperAttributeProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    public MapperAttributeProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_MAPPER, provider, realm, new MapperAttributeProviderConfigMap());
    }

    public MapperAttributeProviderConfig(ConfigurableAttributeProvider cp) {
        super(cp);
    }

    public String getMapperType() {
        return configMap.getType() != null ? configMap.getType() : DefaultAttributesMapper.TYPE;
    }

}
