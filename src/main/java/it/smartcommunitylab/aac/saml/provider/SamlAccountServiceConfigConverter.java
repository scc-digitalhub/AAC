package it.smartcommunitylab.aac.saml.provider;

import org.springframework.core.convert.converter.Converter;

public class SamlAccountServiceConfigConverter
        implements Converter<SamlIdentityProviderConfig, SamlAccountServiceConfig> {

    @Override
    public SamlAccountServiceConfig convert(SamlIdentityProviderConfig source) {
        SamlAccountServiceConfig config = new SamlAccountServiceConfig(source.getAuthority(),
                source.getProvider(),
                source.getRealm());
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
