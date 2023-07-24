package it.smartcommunitylab.aac.openid.provider;

import org.springframework.core.convert.converter.Converter;

public class OIDCAccountServiceConfigConverter
    implements Converter<OIDCIdentityProviderConfig, OIDCAccountServiceConfig> {

    @Override
    public OIDCAccountServiceConfig convert(OIDCIdentityProviderConfig source) {
        OIDCAccountServiceConfig config = new OIDCAccountServiceConfig(
            source.getAuthority(),
            source.getProvider(),
            source.getRealm()
        );
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
