package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.accounts.provider.AccountServiceSettingsMap;
import org.springframework.core.convert.converter.Converter;

public class SpidAccountServiceConfigConverter implements Converter<SpidIdentityProviderConfig, SpidAccountServiceConfig> {
    @Override
    public SpidAccountServiceConfig convert(SpidIdentityProviderConfig source) {
        SpidAccountServiceConfig config = new SpidAccountServiceConfig(source.getAuthority(), source.getProvider(), source.getRealm());
        config.setName(source.getName());
        config.setTitleMap(source.getTitleMap());
        config.setDescriptionMap(source.getDescriptionMap());

        config.setConfigMap(source.getConfigMap());
        config.setVersion(source.getVersion());

        // re-build settings map
        AccountServiceSettingsMap settingsMap = new AccountServiceSettingsMap();
        settingsMap.setPersistence(source.getPersistence());
//        settingsMap.setRepositoryId(source.getRepositoryId());
        config.setSettingsMap(settingsMap);

        return config;
    }
}
