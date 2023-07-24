package it.smartcommunitylab.aac.openid.apple.provider;

import org.springframework.core.convert.converter.Converter;

public class AppleAccountServiceConfigConverter
    implements Converter<AppleIdentityProviderConfig, AppleAccountServiceConfig> {

    @Override
    public AppleAccountServiceConfig convert(AppleIdentityProviderConfig source) {
        AppleAccountServiceConfig config = new AppleAccountServiceConfig(source.getProvider(), source.getRealm());
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
