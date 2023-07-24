package it.smartcommunitylab.aac.internal.provider;

import org.springframework.core.convert.converter.Converter;

public class InternalAccountServiceConfigConverter
    implements Converter<InternalIdentityProviderConfig, InternalAccountServiceConfig> {

    @Override
    public InternalAccountServiceConfig convert(InternalIdentityProviderConfig source) {
        InternalAccountServiceConfig config = new InternalAccountServiceConfig(source.getProvider(), source.getRealm());
        config.setName(source.getName());
        config.setTitleMap(source.getTitleMap());
        config.setDescriptionMap(source.getDescriptionMap());

        // we share the same configMap
        config.setConfigMap(source.getConfigMap());
        config.setRepositoryId(source.getRepositoryId());
        config.setPersistence(source.getPersistence());
        return config;
    }
}
