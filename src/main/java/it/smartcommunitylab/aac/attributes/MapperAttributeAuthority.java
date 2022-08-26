package it.smartcommunitylab.aac.attributes;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.provider.MapperAttributeProvider;
import it.smartcommunitylab.aac.attributes.provider.MapperAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.provider.MapperAttributeProviderConfigMap;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.core.base.AbstractAttributeAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;

@Service
public class MapperAttributeAuthority extends
        AbstractAttributeAuthority<MapperAttributeProvider, MapperAttributeProviderConfig, MapperAttributeProviderConfigMap> {

    public MapperAttributeAuthority(
            AttributeService attributeService,
            AutoJdbcAttributeStore jdbcAttributeStore,
            ProviderConfigRepository<MapperAttributeProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_MAPPER, attributeService, jdbcAttributeStore, registrationRepository);
    }

    @Override
    protected MapperAttributeProvider buildProvider(MapperAttributeProviderConfig config) {
        AttributeStore attributeStore = getAttributeStore(config.getProvider(), config.getPersistence());

        MapperAttributeProvider ap = new MapperAttributeProvider(
                config.getProvider(),
                attributeService, attributeStore,
                config,
                config.getRealm());

        return ap;
    }

}
