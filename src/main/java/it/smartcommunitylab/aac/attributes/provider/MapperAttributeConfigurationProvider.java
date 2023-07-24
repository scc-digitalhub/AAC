package it.smartcommunitylab.aac.attributes.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAttributeConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import org.springframework.stereotype.Service;

@Service
public class MapperAttributeConfigurationProvider
    extends AbstractAttributeConfigurationProvider<MapperAttributeProviderConfigMap, MapperAttributeProviderConfig> {

    public MapperAttributeConfigurationProvider() {
        super(SystemKeys.AUTHORITY_MAPPER);
        setDefaultConfigMap(new MapperAttributeProviderConfigMap());
    }

    @Override
    protected MapperAttributeProviderConfig buildConfig(ConfigurableAttributeProvider cp) {
        return new MapperAttributeProviderConfig(cp, getConfigMap(cp.getConfiguration()));
    }
}
