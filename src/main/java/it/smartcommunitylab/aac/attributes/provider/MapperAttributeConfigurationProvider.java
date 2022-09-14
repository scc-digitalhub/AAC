package it.smartcommunitylab.aac.attributes.provider;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAttributeConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

@Service
public class MapperAttributeConfigurationProvider extends
        AbstractAttributeConfigurationProvider<MapperAttributeProviderConfig, MapperAttributeProviderConfigMap> {

    public MapperAttributeConfigurationProvider() {
        super(SystemKeys.AUTHORITY_MAPPER);
        setDefaultConfigMap(new MapperAttributeProviderConfigMap());
    }

    @Override
    protected MapperAttributeProviderConfig buildConfig(ConfigurableAttributeProvider cp) {
        return new MapperAttributeProviderConfig(cp);
    }

}
